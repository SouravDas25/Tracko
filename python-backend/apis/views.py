# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.http import JsonResponse

import language


# Create your views here.

def index(request):
    d = {}
    if "text" in request.GET:
        text = request.GET["text"]
        lang = language.Language(text)
        d = lang.getDict()
    return JsonResponse(d, safe=False)
