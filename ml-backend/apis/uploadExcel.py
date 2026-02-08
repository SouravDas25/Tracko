import pandas as pd
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt


@csrf_exempt
def uploadData(request):
    print(request.FILES)
    if 'excel' in request.FILES:
        excel = ExcelUpload(request.FILES['excel'].name, request.FILES['excel'])
        return HttpResponse(excel.process())
    return HttpResponse("No File Uploaded")


class ExcelUpload(object):

    def __init__(self, name, file):
        self.name = name
        self.type = None
        self.file = file
        s = name.split(".")
        if len(s) > 1:
            self.type = s[1]

    def process(self):
        print(self.file)
        if self.type == "csv":
            xl = pd.read_csv(self.file)
        else:
            xl = pd.read_excel(self.file)
        # print(xl.head(5))

        return xl
