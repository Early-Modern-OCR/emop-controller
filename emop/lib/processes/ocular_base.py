import logging
import os
import shutil
from emop.lib.processes.processes_base import ProcessesBase
from emop.lib.utilities import get_max_rss, recursive_copy

logger = logging.getLogger('emop')


class OcularBase(ProcessesBase):

    def __init__(self, job):
        super(OcularBase, self).__init__(job)
        self.jar = os.getenv("OCULAR")
        self.extra_command_parameters = self.job.extra_command_parameters
        self.input_font_path = self.job.input_font_path
        self.input_lm_path = self.job.input_lm_path
        self.input_gsm_path = self.job.input_gsm_path
        self.output_font_path = self.job.output_font_path
        self.output_lm_path = self.job.output_lm_path
        self.output_gsm_path = self.job.output_gsm_path
        self.output_path = self.job.output_dir
        self.input_doc_list_path = self.job.input_doc_list_path
        self.images = []
        # Assumes that all images have same parent directory and that this directory
        # name is used as directory name in outputPath for transcription
        _image_dirname = os.path.basename(os.path.dirname(self.job.image_path))
        self.transcription_dir = os.path.join(self.output_path, 'all_transcriptions')
        self.transcribed_output_path = os.path.join(self.transcription_dir, _image_dirname)
        self.java_max_heap = self.calculate_max_heap()

    def calculate_max_heap(self):
        """ Determine max heap

        The value is based on max RSS soft ulimit.
        The max RSS limit is then converted from bytes to MB then
        divided by 2000 to give roughly half the max RSS in GB

        Returns:
            str: The java -Xmx with appropriate value added
        """
        _max_rss_bytes = get_max_rss()
        _max_rss_mb = _max_rss_bytes / 1024**2
        _max_heap = _max_rss_mb / 2000
        _java_max_heap = "-Xmx%sg" % _max_heap
        return _java_max_heap

    def generate_input_doc_list(self):
        for j in self.job.jobs:
            self.images.append(j.image_path)
        logger.debug("%s: Writing inputDocListPath %s Contents:\n%s", self.__class__.__name__, self.input_doc_list_path, "\n".join(self.images))
        with open(self.input_doc_list_path, 'w') as f:
            for i in self.images:
                f.write("%s\n" % i)

    # Unused but left incase it ever became useful
    # def copy_previous_results(self):
    #     if self.previous_output_path:
    #         logger.debug("%s: Previous output path defined: %s", self.__class__.__name__, self.previous_output_path)
    #         if os.path.isdir(self.previous_output_path):
    #             logger.debug("%s: Copying %s -> %s", self.__class__.__name__, self.previous_output_path, self.output_path)
    #             try:
    #                 exclude = [self.input_font_path]
    #                 if self.input_gsm_path:
    #                     exclude.append(self.input_gsm_path)
    #                 recursive_copy(src=self.previous_output_path, dest=self.output_path, exclude=exclude)
    #             except Exception as err:
    #                 logger.error("%s: Failed to copy: %s", self.__class__.__name__, str(err))
    #                 return False
    #             # The trained font should now be in outputPath, so use that file instead??
    #             _copied_font_path = os.path.join(self.output_path, self.output_font_name)
    #             if os.path.isfile(_copied_font_path):
    #                 logger.info("%s: Using copied font at %s", self.__class__.__name__, _copied_font_path)
    #                 self.input_font_path = _copied_font_path
    #             else:
    #                 logger.info("%s: Copied font not found at %s, will use %s", self.__class__.__name__, _copied_font_path, self.input_font_path)
    #         else:
    #             logger.info("%s: Previous output path not found: %s", self.__class__.__name__, self.previous_output_path)
    #             return False
    #     else:
    #         logger.info("%s: Previous output path not defined", self.__class__.__name__)
    #         return False
    #     return True
