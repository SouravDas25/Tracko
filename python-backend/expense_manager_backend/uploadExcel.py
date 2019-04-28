
import pandas as pd
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt


@csrf_exempt
def uploadData(request):
    print(request.FILES)
    excel = ExcelUpload(request.FILES['excel'])
    return HttpResponse(excel.process())

class ExcelUpload(object):

    def __init__(self,file):
        self.file = file

    def process(self):
        print(self.file)
        xl = pd.read_csv(self.file)
        print(xl.head(5))
        return xl.head(1)