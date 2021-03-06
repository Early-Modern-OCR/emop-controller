from emop.lib.emop_base import EmopBase
from emop.lib.models.emop_model import EmopModel


class EmopFont(EmopModel):

    transfer_attributes = [
        'font_library_path',
        'path',
    ]

    def __init__(self, settings):
        super(self.__class__, self).__init__(settings)
        self._path = None

    def setattrs(self, dictionary):
        self.name = dictionary.get("font_name")
        if dictionary.get("font_library_path"):
            self.path = dictionary.get("font_library_path")
        else:
            self.path = dictionary.get("path")
        self.batch_job_id = dictionary.get("batch_job_id")
        self.work_id = dictionary.get("work_id")

    @property
    def path(self):
        return self._path

    @path.setter
    def path(self, value):
        prefix = self.settings.input_path_prefix
        new_value = EmopBase.add_prefix(prefix=prefix, path=value)
        self._path = new_value
