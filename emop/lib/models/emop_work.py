from emop.lib.models.emop_model import EmopModel


class EmopWork(EmopModel):

    def __init__(self, settings):
        super(self.__class__, self).__init__(settings)

    def setattrs(self, dictionary):
        if dictionary.get("id"):
            self.id = dictionary["id"]
        elif dictionary.get("wks_work_id"):
            self.id = dictionary["wks_work_id"]
        else:
            self.id = None
        self.organizational_unit = dictionary["wks_organizational_unit"]
        self.title = dictionary["wks_title"]
        self.ecco_id = dictionary["wks_ecco_number"]
        self.ecco_directory = dictionary["wks_ecco_directory"]
        self.eebo_id = dictionary["wks_eebo_image_id"]
        if dictionary.get("wks_doc_directory"):
            self.eebo_directory = dictionary["wks_doc_directory"]
        elif dictionary.get("wks_eebo_directory"):
            self.eebo_directory = dictionary["wks_eebo_directory"]
        else:
            self.eebo_directory = None

    def is_ecco(self):
        if self.ecco_directory:
            return True
        else:
            return False
