# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.shortcuts import render
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt
import json

from . import language


# Create your views here.
@csrf_exempt
def index(request):
    d = {}
    if request.method == 'POST':
        data = json.loads(request.body)
        text = data['text']
        lang = language.Language(text)
        d = lang.getDict()
        d['request'] = data
    return JsonResponse(d, safe=False)