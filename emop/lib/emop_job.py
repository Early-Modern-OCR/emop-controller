import os
from emop.lib.emop_base import EmopBase
from emop.lib.models.emop_batch_job import EmopBatchJob
from emop.lib.models.emop_font import EmopFont
from emop.lib.models.emop_page import EmopPage
from emop.lib.models.emop_work import EmopWork
from emop.lib.models.emop_page_result import EmopPageResult
from emop.lib.models.emop_postproc_result import EmopPostprocResult
from emop.lib.utilities import get_temp_dir


class EmopJob(object):

    def __init__(self, job_data, settings, scheduler):
        self.settings = settings
        self.scheduler = scheduler
        self.parse_data(data=job_data)
        self.output_root_dir = EmopBase.add_prefix(self.settings.output_path_prefix, self.settings.ocr_root)
        self.temp_dir = get_temp_dir()
        self.image_path = self.page.image_path
        # The values below rely on values set above
        self.output_dir = self.get_output_dir()
        self.txt_file = self.output_file("txt")
        self.xml_file = self.output_file("xml")
        self.hocr_file = self.output_file("hocr")
        self.idhmc_txt_file = self.add_filename_suffix(self.txt_file, "IDHMC")
        self.idhmc_xml_file = self.add_filename_suffix(self.xml_file, "IDHMC")
        self.alto_txt_file = self.add_filename_suffix(self.txt_file, "ALTO")
        self.alto_xml_file = self.add_filename_suffix(self.xml_file, "ALTO")

    def parse_data(self, data):
        self.id = data["id"]
        self.batch_job = EmopBatchJob(self.settings)
        self.font = EmopFont(self.settings)
        self.page = EmopPage(self.settings)
        self.work = EmopWork(self.settings)
        self.page_result = EmopPageResult(self.settings)
        self.postproc_result = EmopPostprocResult(self.settings)
        self.batch_job.setattrs(data["batch_job"])
        self.font.setattrs(data["batch_job"]["font"])
        self.page.setattrs(data["page"])
        self.work.setattrs(data["work"])
        self.page_result.set_existing_attrs(data.get("page_result"))
        self.postproc_result.set_existing_attrs(data.get("postproc_result"))
        self.page_result.page_id = self.page.id
        self.page_result.batch_id = self.batch_job.id
        self.postproc_result.page_id = self.page.id
        self.postproc_result.batch_job_id = self.batch_job.id

    def get_output_dir(self):
        """ Provide the job output directory

        Format is the following:
            /<config.ini output_path_prefix><config.ini ocr_root>/<batch ID>/<work ID>

        Example:
            /dh/data/shared/text-xml/IDHMC-OCR/<batch.id>/<work.id>

        Returns:
            str: Output directory path
        """
        path = os.path.join(self.output_root_dir, str(self.batch_job.id), str(self.work.id))
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
