import logging
import os
import shutil
from emop.lib.processes.ocular_base import OcularBase
from emop.lib.utilities import mkdirs_exists_ok, exec_cmd

logger = logging.getLogger('emop')


class OcularTranscribe(OcularBase):

    def __init__(self, job):
        super(self.__class__, self).__init__(job)
        self.ocr_text_paths = []
        self.ocr_xml_paths = []
        # Loop over each of this job's pages and build transcribed output paths
        # These paths are added as results if the file is found
        for j in self.job.jobs:
            _image_basename = os.path.basename(j.image_path)
            _image_name = os.path.splitext(_image_basename)[0]
            _txt_name = "%s_transcription.txt" % _image_name
            _alto_name = "%s.alto.xml" % _image_name
            _txt_path = os.path.join(self.transcribed_output_path, _txt_name)
            _alto_path = os.path.join(self.transcribed_output_path, _alto_name)
            self.ocr_text_paths.append(_txt_path)
            self.ocr_xml_paths.append(_alto_path)

    def should_run(self):
        _ret = False
        for path in self.ocr_text_paths:
            if not os.path.isfile(path):
                _ret = True
        for path in self.ocr_xml_paths:
            if not os.path.isfile(path):
                _ret = True
        return _ret

    def run(self):
        self.generate_input_doc_list()

        if not self.input_font_path:
            stderr = "No input font path could be determined"
            return self.results(stdout=None, stderr=stderr, exitcode=1)
        if not os.path.isfile(self.input_font_path):
            stderr = "Could not find input font path %s" % self.input_font_path
            return self.results(stdout=None, stderr=stderr, exitcode=1)

        # Create output parent directory if it doesn't exist
        if not os.path.isdir(self.output_path):
            mkdirs_exists_ok(self.output_path)

        cmd = [
            "java", self.java_max_heap,
            "-Done-jar.main.class=edu.berkeley.cs.nlp.ocular.main.Transcribe",
            "-jar", self.jar,
            "-outputPath", self.output_path,
            "-inputDocListPath", self.input_doc_list_path,
            "-inputFontPath", self.input_font_path,
            "-inputLmPath", self.input_lm_path,
            "-inputGsmPath", self.input_gsm_path,
            "-allowGlyphSubstitution", "true",
            "-skipAlreadyTranscribedDocs", 'true',
            "-emissionEngine", self.job.settings.ocular_emission_engine,
        ]
        if self.extra_command_parameters:
            cmd = cmd + self.extra_command_parameters
        proc = exec_cmd(cmd)

        if proc.exitcode != 0:
            return self.results(stdout=proc.stdout, stderr=proc.stderr, exitcode=proc.exitcode)

        # Loop over each of this job's pages and build transcribed output paths
        # These paths are added as results if the file is found
        for j in self.job.jobs:
            _image_basename = os.path.basename(j.image_path)
            _image_name = os.path.splitext(_image_basename)[0]
            _txt_name = "%s_transcription.txt" % _image_name
            _alto_name = "%s.alto.xml" % _image_name
            _txt_path = os.path.join(self.transcribed_output_path, _txt_name)
            _alto_path = os.path.join(self.transcribed_output_path, _alto_name)
            if os.path.isfile(_txt_path):
                j.page_result.ocr_text_path = _txt_path
            if os.path.isfile(_alto_path):
                j.page_result.ocr_xml_path = _alto_path
        # Add extra transfers
        if os.path.isdir(self.transcription_dir):
            self.job.extra_transfers.append(self.transcription_dir)

        return self.results(stdout=None, stderr=None, exitcode=0)
