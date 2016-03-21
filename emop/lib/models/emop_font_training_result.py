from emop.lib.emop_base import EmopBase
from emop.lib.models.emop_model import EmopModel


class EmopFontTrainingResult(EmopModel):

    PROPERTIES = [
        'work_id',
        'batch_job_id',
        'font_path',
        'language_model_path',
        'glyph_substitution_model_path'
    ]

    def __init__(self, settings):
        super(self.__class__, self).__init__(settings)
        self._font_path = None
        self._language_model_path = None
        self._glyph_substitution_model_path = None
        for _property in self.PROPERTIES:
            setattr(self, _property, None)
            setattr(self, ("%s_exists" % _property), False)

    def set_existing_attrs(self, dictionary):
        if dictionary:
            for _property in self.PROPERTIES:
                if _property in dictionary:
                    setattr(self, ("%s_exists" % _property), True)

    def to_dict(self):
        _dict = {}
        for _property in self.PROPERTIES:
            value = getattr(self, _property)
            if value is None:
                continue
            _dict[_property] = value
        return _dict

    def has_data(self):
        keys = list(self.to_dict().keys())
        data_keys = set(keys) - set(["work_id", "batch_job_id"])
        if len(data_keys) >= 1:
            return True
        else:
            return False

    @property
    def font_path(self):
        """The path to trained font result"""
        return self._font_path

    @font_path.setter
    def font_path(self, value):
        prefix = self.settings.output_path_prefix
        new_value = EmopBase.remove_prefix(prefix=prefix, path=value)
        self._font_path = new_value

    @property
    def language_model_path(self):
        """The path to trained font result"""
        return self._language_model_path

    @language_model_path.setter
    def language_model_path(self, value):
        prefix = self.settings.output_path_prefix
        new_value = EmopBase.remove_prefix(prefix=prefix, path=value)
        self._language_model_path = new_value

    @property
    def glyph_substitution_model_path(self):
        """The path to trained font result"""
        return self._glyph_substitution_model_path

    @glyph_substitution_model_path.setter
    def glyph_substitution_model_path(self, value):
        prefix = self.settings.output_path_prefix
        new_value = EmopBase.remove_prefix(prefix=prefix, path=value)
        self._glyph_substitution_model_path = new_value
