import json
import os
from unittest import TestCase

class TestFirstBooksTrain(TestCase):
    def setUp(self):
        test_root = os.path.dirname(__file__)
        fixture_dir = os.path.join(test_root, 'fixtures')
        fixture_file = os.path.join(fixture_dir, 'firstbooks-train-output.json')
        with open(fixture_file) as datafile:
            self.fixture_data = json.load(datafile)

        self.test_file = os.path.join(test_root, 'payload/output/completed/firstbooks-train.json')
        with open(self.test_file) as datafile:
            self.test_data = json.load(datafile)

    def get_font_training_results_values(self, key):
        fixture_font_training_results = sorted(self.fixture_data["font_training_results"], key=lambda k: k["batch_job_id"])
        test_font_training_results = sorted(self.test_data["font_training_results"], key=lambda k: k["batch_job_id"])

        fixture_values = [d[key] for d in fixture_font_training_results]
        test_values = [d[key] for d in test_font_training_results]

        return fixture_values, test_values

    def test_job_queues_completed(self):
        job_queues_completed = self.test_data["job_queues"]["completed"]

        self.assertEqual(10, len(job_queues_completed))

    def test_job_queues_failed(self):
        job_queues_failed = self.test_data["job_queues"]["failed"]

        self.assertEqual(0, len(job_queues_failed))

    def test_font_training_result_paths(self):
        font_fixture_path, font_test_path = self.get_font_training_results_values("font_path")
        lm_fixture_path, lm_test_path = self.get_font_training_results_values("language_model_path")
        gsm_fixture_path, gsm_test_path = self.get_font_training_results_values("glyph_substitution_model_path")

        self.assertEqual(font_fixture_path, font_test_path)

class TestFirstBooksTranscribe(TestCase):
    def setUp(self):
        test_root = os.path.dirname(__file__)
        fixture_dir = os.path.join(test_root, 'fixtures')
        fixture_file = os.path.join(fixture_dir, 'firstbooks-transcribe-output.json')
        with open(fixture_file) as datafile:
            self.fixture_data = json.load(datafile)

        self.test_file = os.path.join(test_root, 'payload/output/completed/firstbooks-transcribe.json')
        with open(self.test_file) as datafile:
            self.test_data = json.load(datafile)

    def get_page_results_values(self, key):
        fixture_page_results = sorted(self.fixture_data["page_results"], key=lambda k: k["page_id"])
        test_page_results = sorted(self.test_data["page_results"], key=lambda k: k["page_id"])

        fixture_values = [d[key] for d in fixture_page_results]
        test_values = [d[key] for d in test_page_results]

        return fixture_values, test_values

    def test_job_queues_completed(self):
        job_queues_completed = self.test_data["job_queues"]["completed"]

        self.assertEqual(26, len(job_queues_completed))

    def test_job_queues_failed(self):
        job_queues_failed = self.test_data["job_queues"]["failed"]

        self.assertEqual(0, len(job_queues_failed))

    def test_page_result_paths(self):
        fixture_ocr_text_paths, test_ocr_text_paths = self.get_page_results_values("ocr_text_path")
        fixture_ocr_xml_paths, test_ocr_xml_paths = self.get_page_results_values("ocr_xml_path")

        self.assertEqual(fixture_ocr_text_paths, test_ocr_text_paths)
        self.assertEqual(fixture_ocr_xml_paths, test_ocr_xml_paths)
