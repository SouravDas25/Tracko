# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.shortcuts import render
from django.http import JsonResponse

import nltk
from nltk.corpus import stopwords
from nltk.stem.porter import *
from nltk.chunk import tree2conlltags

import language


# Create your views here.

def index(request):
    d = {}
    if "text" in request.GET:
        text = request.GET["text"]
        lang = language.Language()
        lang.process(text)
        d = lang.getDict()
    return JsonResponse(d, safe=False)
