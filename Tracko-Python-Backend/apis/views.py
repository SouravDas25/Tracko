# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import json

from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt

from . import language


# Create your views here.
@csrf_exempt
def index(request):
    d = {}
    if request.method == 'POST':
        data = json.loads(request.body)
        address = data['address']
        text = data['text']
        lang = language.Language(text, address)
        d = lang.getDict()
        d['request'] = data
    return JsonResponse(d, safe=False)