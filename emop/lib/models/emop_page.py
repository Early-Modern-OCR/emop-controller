from emop.lib.emop_base import EmopBase
from emop.lib.models.emop_model import EmopModel
from emop.lib.models.emop_work import EmopWork


class EmopPage(EmopModel):

    transfer_attributes = [
        'pg_image_path',
        'pg_ground_truth_file',
    ]

    def __init__(self, settings):
        super(self.__class__, self).__init__(settings)
        self._ground_truth_file = None
        self._image_path = None

    def setattrs(self, dictionary):
        self.work = work = EmopWork(self.settings)
        self.work.setattrs(dictionary["work"])
        self.id = dictionary["id"]
        self.number = dictionary["pg_ref_number"]
        self.image_path = dictionary["pg_image_path"]
        self.gale_ocr_file = dictionary["pg_gale_ocr_file"]
        self.ground_truth_file = dictionary["pg_ground_truth_file"]

    def hasGroundTruth(self):
        if self.ground_truth_file:
            return True
        else:
            return False

    def hasGaleText(self):
        if self.gale_ocr_file:
            return True
        else:
            return False

    @property
    def ground_truth_file(self):
        """The path to the page's ground truth file"""
        return self._ground_truth_file

    @ground_truth_file.setter
    def ground_truth_file(self, value):
        prefix = self.settings.input_path_prefix
        new_value = EmopBase.add_prefix(prefix=prefix, path=value)
        self._ground_truth_file = new_value

    @property
    def image_path(self):
        """The path to the page's ground truth file"""
        return self._image_path

    @image_path.setter
    def image_path(self, value):
        """Determine the full path of an image

        This function generates an image path based on value of image path for a page.
        If a page has no image path then one is generated.

        ECCO image path format:
            eeco_directory/<eeco ID> + <4 digit page ID> + 0.[tif | TIF]
        EEBO image path format:
            eebo_directory/<eebo ID>.000.<0-100>.[tif | TIF]

        Args:
            value (str): Path to page image from API

        Returns:
            str: Path to the page image
            None is returned if no path could be determined which constitutes an error
        """
        if value:
            self._image_path = EmopBase.add_prefix(self.settings.input_path_prefix, value)
        # image path was not provided by API so one will be generated
        else:
            # EECO
            if self.work.is_ecco():
                img = "%s/%s%04d0.tif" % (self.work.ecco_directory, self.work.ecco_id, self.number)
                image_path = EmopBase.add_prefix(self.settings.input_path_prefix, img)
                image_path_upcase = image_path.replace(".tif", ".TIF")
                if os.path.isfile(image_path):
                    self._image_path = image_path
                elif os.path.isfile(image_path_upcase):
                    self._image_path = image_path_upcase
            # EEBO
            else:
                for i in xrange(101):
                    img = "%s/%05d.000.%03d.tif" % (self.work.eebo_directory, self.number, i)
                    image_path = EmopBase.add_prefix(self.settings.input_path_prefix, img)
                    image_path_upcase = image_path.replace(".tif", ".TIF")
                    if os.path.isfile(image_path):
                        self._image_path = image_path
                    elif os.path.isfile(image_path_upcase):
                        self._image_path = image_path_upcase
                    else:
                        continue
