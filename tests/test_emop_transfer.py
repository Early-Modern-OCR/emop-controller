import pytest
from unittest import TestCase
from unittest import TestLoader
#from flexmock import flexmock
import os, time
from mock import MagicMock, Mock, patch
from tests.utilities import default_settings, default_config_path, fixture_file, load_fixture_file
from globusonline.transfer import api_client
from emop.lib.transfer.globus import GlobusAPIClient
from emop.emop_transfer import EmopTransfer
from emop.lib.emop_payload import EmopPayload

xfail = pytest.mark.xfail
skipif = pytest.mark.skipif

class TestEmopTransfer(TestCase):
    @pytest.fixture(autouse=True)
    def setup(self, tmpdir):
        self.tmpout = str(tmpdir.mkdir("out"))
        globus_dir = tmpdir.mkdir('globus')
        auth_file = globus_dir.join('globus-auth')
        expiry = int(time.time()) + (60*60*24*365)
        self.fake_goauth_token = 'un=test|tokenid=fake-token-id|expiry=%d' % expiry
        auth_file.write(self.fake_goauth_token)
        settings = default_settings()
        settings.globus_auth_file = str(auth_file)
        with patch('emop.emop_transfer.GlobusAPIClient') as globus_class:
            mock_globus = GlobusAPIClient(settings=settings)
            globus_class.return_value = mock_globus
            self.transfer = EmopTransfer(config_path=default_config_path())

    def setUp(self):
        pass

    def tearDown(self):
        pass

    def test_stage_in_files_1(self):
        files = ['/dne/file.txt']
        data = [
            {'src': '/dne/file.txt', 'dest': '/fdata/idhmc/emop-input/dne/file.txt'}
        ]
        self.transfer.start = MagicMock()
        self.transfer.stage_in_files(files)
        self.transfer.start.assert_called_once_with(src='idhmc#data', dest='tamu#brazos', data=data, label='emop-stage-in-files', wait=False)

    def test_stage_in_data_1(self):
        data = load_fixture_file('job_queues_1.json')
        expected_files = [
            "/data/shared/text-xml/EEBO-TCP-pages-text/e0006/40099/2.txt",
            "/data/eebo/e0006/40099/00001.000.001.tif",
            "/data/eebo/e0006/40099/00002.000.001.tif",
            "/data/shared/text-xml/EEBO-TCP-pages-text/e0006/40099/1.txt",
        ]
        self.transfer.stage_in_files = MagicMock()
        self.transfer.stage_in_data(data['results'])
        self.transfer.stage_in_files.assert_called_once_with(files=expected_files, wait=False)

    def test_stage_in_proc_ids(self):
        self.transfer.settings.payload_input_path = os.path.dirname(fixture_file('input_payload_2.json'))
        expected_data = [
            {
                'src': '/data/shared/text-xml/EEBO-TCP-pages-text/e0006/40099/2.txt',
                'dest': '/fdata/idhmc/emop-input/data/shared/text-xml/EEBO-TCP-pages-text/e0006/40099/2.txt',
            },
            {
                'src': '/data/eebo/e0006/40099/00001.000.001.tif',
                'dest': '/fdata/idhmc/emop-input/data/eebo/e0006/40099/00001.000.001.tif',
            },
            {
                'src': '/data/eebo/e0006/40099/00002.000.001.tif',
                'dest': '/fdata/idhmc/emop-input/data/eebo/e0006/40099/00002.000.001.tif',
            },
            {
                'src': '/data/shared/text-xml/EEBO-TCP-pages-text/e0006/40099/1.txt',
                'dest': '/fdata/idhmc/emop-input/data/shared/text-xml/EEBO-TCP-pages-text/e0006/40099/1.txt',
            },
        ]

        self.transfer.start = MagicMock()
        self.transfer.stage_in_proc_ids(proc_ids=['input_payload_2'])
        self.transfer.start.assert_called_once_with(src='idhmc#data', dest='tamu#brazos', data=expected_data, label='emop-stage-in-files', wait=False)
        

    def test_stage_out_proc_id_1(self):
        self.transfer.settings.payload_output_path = os.path.dirname(fixture_file('output_payload_1.json'))
        payload = EmopPayload(self.transfer.settings, 'output_payload_1')
        expected_data = [
            {
                'src': '/fdata/idhmc/emop-output/data/shared/text-xml/IDHMC-ocr/17/152141/1_ALTO.txt',
                'dest': '/data/shared/text-xml/IDHMC-ocr/17/152141/1_ALTO.txt',
            },
            {
                'src': '/fdata/idhmc/emop-output/data/shared/text-xml/IDHMC-ocr/17/152141/1.txt',
                'dest': '/data/shared/text-xml/IDHMC-ocr/17/152141/1.txt',
            },
            {
                'src': '/fdata/idhmc/emop-output/data/shared/text-xml/IDHMC-ocr/17/152141/1_ALTO.xml',
                'dest': '/data/shared/text-xml/IDHMC-ocr/17/152141/1_ALTO.xml',
            },
            {
                'src': '/fdata/idhmc/emop-output/data/shared/text-xml/IDHMC-ocr/17/152141/1.xml',
                'dest': '/data/shared/text-xml/IDHMC-ocr/17/152141/1.xml',
            }
        ]
        self.transfer.start = MagicMock(return_value='000-000-001')
        retval = self.transfer.stage_out_proc_id('output_payload_1')
        self.transfer.start.assert_called_once_with(src='tamu#brazos', dest='idhmc#data', data=expected_data, label='emop-stage-out-output_payload_1', wait=False)
        self.assertEqual('000-000-001', retval)

    def test_stage_out_proc_id_2(self):
        self.transfer.settings.payload_completed_path = os.path.dirname(fixture_file('output_payload_1.json'))
        payload = EmopPayload(self.transfer.settings, 'output_payload_1')
        expected_data = [
            {
                'src': '/fdata/idhmc/emop-output/data/shared/text-xml/IDHMC-ocr/17/152141/1_ALTO.txt',
                'dest': '/data/shared/text-xml/IDHMC-ocr/17/152141/1_ALTO.txt',
            },
            {
                'src': '/fdata/idhmc/emop-output/data/shared/text-xml/IDHMC-ocr/17/152141/1.txt',
                'dest': '/data/shared/text-xml/IDHMC-ocr/17/152141/1.txt',
            },
            {
                'src': '/fdata/idhmc/emop-output/data/shared/text-xml/IDHMC-ocr/17/152141/1_ALTO.xml',
                'dest': '/data/shared/text-xml/IDHMC-ocr/17/152141/1_ALTO.xml',
            },
            {
                'src': '/fdata/idhmc/emop-output/data/shared/text-xml/IDHMC-ocr/17/152141/1.xml',
                'dest': '/data/shared/text-xml/IDHMC-ocr/17/152141/1.xml',
            }
        ]
        self.transfer.start = MagicMock(return_value='000-000-001')
        retval = self.transfer.stage_out_proc_id('output_payload_1')
        self.transfer.start.assert_called_once_with(src='tamu#brazos', dest='idhmc#data', data=expected_data, label='emop-stage-out-output_payload_1', wait=False)
        self.assertEqual('000-000-001', retval)

    def test_stage_out_proc_id_3(self):
        payload = EmopPayload(self.transfer.settings, 'output_payload_1')
        payload.completed_output_exists = MagicMock()
        payload.completed_output_exists.return_value = False
        payload.output_exists = MagicMock()
        payload.output_exists.return_value = False
        retval = self.transfer.stage_out_proc_id('output_payload_1')
        self.assertEqual('', retval)

    def test_stage_out_proc_id_4(self):
        self.transfer.settings.payload_output_path = os.path.dirname(fixture_file('invalid.json'))
        payload = EmopPayload(self.transfer.settings, 'invalid')
        retval = self.transfer.stage_out_proc_id('invalid')
        self.assertEqual('', retval)

    def test_check_endpoints_1(self):
        self.transfer._check_activation = MagicMock()
        self.transfer._check_activation.side_effect = [False, False, False, False]
        self.transfer.globus.autoactivate = MagicMock()
        self.transfer.globus.get_activate_url = MagicMock(return_value="https://globus.org/activate?ep=go%23ep1&ep_ids=foobar")
        retval = self.transfer.check_endpoints()
        self.assertEqual(False, retval)

    def test_check_endpoints_2(self):
        self.transfer._check_activation = MagicMock()
        self.transfer._check_activation.side_effect = [True, True]
        retval = self.transfer.check_endpoints()
        self.assertEqual(True, retval)

    def test_start_1(self):
        transfer = api_client.Transfer('test', 'go#ep1', 'go#ep2')
        self.transfer.globus.create_transfer = MagicMock()
        self.transfer.globus.create_transfer.return_value = transfer
        self.transfer.globus.send_transfer = MagicMock()
        self.transfer.globus.send_transfer.return_value = 'task-id'
        data = [{'src': '/dne/file1', 'dest': '/dne/file1'}]
        self.transfer.start('go#ep1', 'go#ep2', data)
        self.assertEqual(1, len(transfer.items))
        self.assertEqual(data[0]['src'], transfer.items[0]['source_path'])
        self.assertEqual(data[0]['dest'], transfer.items[0]['destination_path'])

    @skipif(True, reason="Not yet implemented")
    def test_ls(self):
        pass

    @skipif(True, reason="Not yet implemented")
    def test_display_task(self):
        pass

    def test__get_stage_in_files_from_data_1(self):
        data = [
            {
                "page": {
                    "pg_image_path": '/dne/page1.txt',
                    "pg_ground_truth_file": '/dne/gt1.txt',
                    "pg_foo": "pg_bar",
                },
                "work": {
                    "wk_foo": "wk_bar",
                }
            },
            {
                "page": {
                    "pg_image_path": '/dne/page2.txt',
                    "pg_ground_truth_file": '/dne/gt2.txt',
                    "pg_foo": "pg_bar",
                },
                "work": {
                    "wk_foo": "wk_bar",
                }
            },
        ]
        expected = [
            '/dne/page1.txt',
            '/dne/gt1.txt',
            '/dne/gt2.txt',
            '/dne/page2.txt',
        ]
        retval = self.transfer._get_stage_in_files_from_data(data=data)
        self.assertEqual(expected, retval)

    def test__get_stage_in_files_from_data_2(self):
        data = [
            {
                "page": {
                    "pg_image_path": '/dne/page1.txt',
                    "pg_ground_truth_file": '/dne/gt1.txt',
                    "pg_foo": "pg_bar",
                },
                "work": {
                    "wk_foo": "wk_bar",
                }
            },
            {
                "page": {
                    "pg_image_path": '/dne/page2.txt',
                    "pg_ground_truth_file": None,
                    "pg_foo": "pg_bar",
                },
                "work": {
                    "wk_foo": "wk_bar",
                }
            },
        ]
        expected = [
            '/dne/page1.txt',
            '/dne/gt1.txt',
            '/dne/page2.txt',
        ]
        retval = self.transfer._get_stage_in_files_from_data(data=data)
        self.assertEqual(expected, retval)

    def test__get_stage_out_data_1(self):
        data = {
            "job_queues": {"completed": [1,2,3], "failed": []},
            "page_results": [
                {"page_id": 1, "batch_id": 2, "ocr_text_path": "/dne/1.txt", "ocr_xml_path": "/dne/1.xml"}
            ],
            "font_training_results": [
                {"work_id": 1, "batch_job_id": 2, "font_path": "/dne/font", "language_model_path": "/dne/lm", "glyph_substitution_model_path": "/dne/gsm"}
            ]
        }
        expected = [
            {'dest': '/dne/1.txt', 'src': '/fdata/idhmc/emop-output/dne/1.txt'},
            {'dest': '/dne/1.xml', 'src': '/fdata/idhmc/emop-output/dne/1.xml'},
            {'dest': '/dne/font', 'src': '/fdata/idhmc/emop-output/dne/font'},
            {'dest': '/dne/lm', 'src': '/fdata/idhmc/emop-output/dne/lm'},
            {'dest': '/dne/gsm', 'src': '/fdata/idhmc/emop-output/dne/gsm'},
        ]
        retval = self.transfer._get_stage_out_data(data=data)
        self.maxDiff = None
        self.assertEqual(len(expected), len(retval))
        self.assertEqual(sorted(expected), sorted(retval))

    def test__get_stage_out_data_2(self):
        data = {
            "job_queues": {"completed": [1], "failed": []},
            "page_results": [
                {"page_id": 1, "batch_id": 2, "ocr_text_path": os.path.join(self.tmpout, "1.txt"), "ocr_xml_path": os.path.join(self.tmpout, "1.xml")}
            ],
            "font_training_results": [
                {"work_id": 1, "batch_job_id": 2,
                "font_path": os.path.join(self.tmpout, "font"),
                "language_model_path": os.path.join(self.tmpout, "lm"),
                "glyph_substitution_model_path": os.path.join(self.tmpout, "gsm")}
            ],
            "extra_transfers": [self.tmpout]
        }
        expected = [
            {'dest': self.tmpout, 'src': os.path.join('/fdata/idhmc/emop-output', self.tmpout), 'recursive': True},
        ]
        retval = self.transfer._get_stage_out_data(data=data)
        self.assertEqual(expected, retval)


def suite():
    return TestLoader().loadTestsFromTestCase(TestEmopTransfer)
