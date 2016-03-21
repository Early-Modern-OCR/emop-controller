import logging
import os
from emop.lib.processes.ocular_base import OcularBase
from emop.lib.utilities import mkdirs_exists_ok, exec_cmd

logger = logging.getLogger('emop')


class OcularFontTraining(OcularBase):

    def __init__(self, job):
        super(self.__class__, self).__init__(job)

    def should_run(self):
        if (os.path.isfile(self.output_font_path)
                and os.path.isfile(self.output_lm_path)
                and os.path.isfile(self.output_gsm_path)):
            return False
        else:
            return True

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
            "-Done-jar.main.class=edu.berkeley.cs.nlp.ocular.main.TrainFont",
            "-jar", self.jar,
            "-outputPath", self.output_path,
            "-inputDocListPath", self.input_doc_list_path,
            "-inputFontPath", self.input_font_path,
            "-inputLmPath", self.input_lm_path,
            "-inputGsmPath", self.input_gsm_path,
            #"-numDocs", str((len(self.images))),
            "-outputFontPath", self.output_font_path,
            "-outputLmPath", self.output_lm_path,
            "-outputGsmPath", self.output_gsm_path,
            "-continueFromLastCompleteIteration", "true",
            "-allowGlyphSubstitution", "true",
            "-updateLM", "true",
            "-updateGsm", "true",
            "-emissionEngine", self.job.settings.ocular_emission_engine,
        ]
        if self.extra_command_parameters:
            cmd = cmd + self.extra_command_parameters
        proc = exec_cmd(cmd)

        if proc.exitcode != 0:
            return self.results(stdout=proc.stdout, stderr=proc.stderr, exitcode=proc.exitcode)

        # Only set font_training_result on one page (job) since this is a per-work result
        if os.path.isfile(self.output_font_path):
            self.job.font_training_result.font_path = self.output_font_path
        if os.path.isfile(self.output_lm_path):
            self.job.font_training_result.language_model_path = self.output_lm_path
        if os.path.isfile(self.output_gsm_path):
            self.job.font_training_result.glyph_substitution_model_path = self.output_gsm_path

        return self.results(stdout=None, stderr=None, exitcode=0)
