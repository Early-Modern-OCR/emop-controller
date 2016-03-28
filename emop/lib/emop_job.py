import logging
import os
import shlex
from emop.lib.emop_base import EmopBase
from emop.lib.models.emop_batch_job import EmopBatchJob
from emop.lib.models.emop_font import EmopFont
from emop.lib.models.emop_language_model import EmopLanguageModel
from emop.lib.models.emop_glyph_substitution_model import EmopGlyphSubstitutionModel
from emop.lib.models.emop_page import EmopPage
from emop.lib.models.emop_work import EmopWork
from emop.lib.models.emop_page_result import EmopPageResult
from emop.lib.models.emop_postproc_result import EmopPostprocResult
from emop.lib.models.emop_font_training_result import EmopFontTrainingResult
from emop.lib.utilities import get_temp_dir

logger = logging.getLogger('emop')


class EmopJob(object):

    def __init__(self, job_data, settings, scheduler):
        self.settings = settings
        self.scheduler = scheduler
        self.extra_transfers = []
        self.parse_data(data=job_data)
        self.output_root_dir = EmopBase.add_prefix(self.settings.output_path_prefix, self.settings.ocr_root)
        self.temp_dir = get_temp_dir()
        self.image_path = self.page.image_path
        # The values below rely on values set above
        self.output_dir = self.get_output_dir(batch_id=self.batch_job.id, work_id=self.work.id)
        self.txt_file = self.output_file("txt")
        self.xml_file = self.output_file("xml")
        self.hocr_file = self.output_file("hocr")
        self.idhmc_txt_file = self.add_filename_suffix(self.txt_file, "IDHMC")
        self.idhmc_xml_file = self.add_filename_suffix(self.xml_file, "IDHMC")
        self.alto_txt_file = self.add_filename_suffix(self.txt_file, "ALTO")
        self.alto_xml_file = self.add_filename_suffix(self.xml_file, "ALTO")
        # Ocular specific items
        if self.batch_job.ocr_engine == 'ocular':
            self.input_font_path = self.font.path
            self.input_lm_path = self.language_model.path
            self.input_gsm_path = self.glyph_substitution_model.path
            _base_output_name = "work-%s-batch-%s" % (self.work.id, self.batch_job.id)
            self.output_font_path = os.path.join(self.output_dir, "%s.fontser" % _base_output_name)
            self.output_lm_path = os.path.join(self.output_dir, "%s.lmser" % _base_output_name)
            self.output_gsm_path = os.path.join(self.output_dir, "%s.gsmser" % _base_output_name)
            self.input_doc_list_path = os.path.join(self.temp_dir, "batch-%s-work-%s-pages-images.txt" % (str(self.batch_job.id), str(self.work.id)))
        # Extra command parameters that are passed to OCR application
        _extra_command_parameters = self.batch_job.parameters
        if _extra_command_parameters and isinstance(_extra_command_parameters, basestring):
            self.extra_command_parameters = shlex.split(_extra_command_parameters)
        else:
            self.extra_command_parameters = None

    def parse_data(self, data):
        self.id = data["id"]
        self.batch_job = EmopBatchJob(self.settings)
        self.font = EmopFont(self.settings)
        self.language_model = EmopLanguageModel(self.settings)
        self.glyph_substitution_model = EmopGlyphSubstitutionModel(self.settings)
        self.page = EmopPage(self.settings)
        self.work = EmopWork(self.settings)
        self.page_result = EmopPageResult(self.settings)
        self.postproc_result = EmopPostprocResult(self.settings)
        self.font_training_result = EmopFontTrainingResult(self.settings)
        self.batch_job.setattrs(data["batch_job"])
        if "font" in data:
            self.font.setattrs(data["font"])
        elif "font" in data["batch_job"]:
            self.font.setattrs(data["batch_job"]["font"])
        else:
            self.font.setattrs({})
        if "language_model" in data:
            self.language_model.setattrs(data["language_model"])
        elif "language_model" in data["batch_job"]:
            self.language_model.setattrs(data["batch_job"]["language_model"])
        else:
            self.language_model.setattrs({})
        if "glyph_substitution_model" in data:
            self.glyph_substitution_model.setattrs(data["glyph_substitution_model"])
        elif "glyph_substitution_model" in data["batch_job"]:
            self.glyph_substitution_model.setattrs(data["batch_job"]["glyph_substitution_model"])
        else:
            self.language_model.setattrs({})
        self.page.setattrs(data["page"])
        self.work.setattrs(data["work"])
        self.page_result.set_existing_attrs(data.get("page_result"))
        self.postproc_result.set_existing_attrs(data.get("postproc_result"))
        self.postproc_result.set_existing_attrs(data.get("postproc_result"))
        self.page_result.page_id = self.page.id
        self.page_result.batch_id = self.batch_job.id
        self.postproc_result.page_id = self.page.id
        self.postproc_result.batch_job_id = self.batch_job.id
        self.font_training_result.work_id = self.work.id
        self.font_training_result.batch_job_id = self.batch_job.id

    def get_output_dir(self, batch_id, work_id):
        """ Provide the job output directory

        Format is the following:
            /<config.ini output_path_prefix><config.ini ocr_root>/<batch ID>/<work ID>

        Example:
            /dh/data/shared/text-xml/IDHMC-OCR/<batch.id>/<work.id>

        Returns:
            str: Output directory path
        """
        path = os.path.join(self.output_root_dir, str(batch_id), str(work_id))
        return path

    def output_file(self, fmt):
        """ Provide the job output file name

        Format is the following:
            <output_dir>/<page.number>.<fmt>

        Example:
            <output_dir>/<page.number>.<fmt>

        Args:
            fmt (str): File format (extension) for file path

        Returns:
            str: Output file path
        """
        filename = "%s.%s" % (self.page.number, str(fmt).lower())
        path = os.path.join(self.output_dir, filename)
        return path

    def add_filename_suffix(self, file, suffix):
        """ Add filename suffix

        This function adds a suffix to a filename before the extension

        Example:
            add_filename_suffix('5.xml', 'IDHMC')
            5.xml -> 5_IDHMC.xml

        Args:
            file (str): File name to add suffix
            suffix (str): The suffix to add

        Returns:
            str: The filename with suffix added before extension
        """
        filename, ext = os.path.splitext(file)
        return "%s_%s%s" % (filename, suffix, ext)
