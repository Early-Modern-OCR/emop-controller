from emop.models.emop_model import EmopModel


class EmopBatchJob(EmopModel):

    def __init__(self):
        super(self.__class__, self).__init__()

    def setattrs(self, dictionary):
        self.id = dictionary["id"]
        self.name = dictionary["name"]
        self.notes = dictionary["notes"]
        self.parameters = dictionary["parameters"]
        self.job_type = dictionary["job_type"]["name"].lower()
        self.ocr_engine = dictionary["ocr_engine"]["name"].lower()
