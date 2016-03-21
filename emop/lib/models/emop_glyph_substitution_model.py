from emop.lib.emop_base import EmopBase
from emop.lib.models.emop_model import EmopModel


class EmopGlyphSubstitutionModel(EmopModel):

    transfer_attributes = [
        'path',
    ]

    def __init__(self, settings):
        super(self.__class__, self).__init__(settings)
        self._path = None

    def setattrs(self, dictionary):
        self.name = dictionary.get("name")
        self.path = dictionary.get("path")

    @property
    def path(self):
        return self._path

    @path.setter
    def path(self, value):
        prefix = self.settings.input_path_prefix
        new_value = EmopBase.add_prefix(prefix=prefix, path=value)
        self._path = new_value
