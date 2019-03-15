# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.shortcuts import render
from django.http import JsonResponse


import nltk
from nltk.corpus import stopwords
from nltk.stem.porter import *
from nltk.chunk import tree2conlltags

# Create your views here.


def index(request):
        
    stemmer = PorterStemmer()

    example_text = "Rs 103.65 debited form **9562 a/c on 11-03-19 via VPA Google@ybl."

    words = nltk.word_tokenize(example_text)

    stops = set(stopwords.words("english")) 
    meaningful_words = [w for w in words if not w in stops]
    singles = [stemmer.stem(word) for word in meaningful_words]

    tags = nltk.pos_tag(singles)


    print(words)
    print(meaningful_words)
    print(singles)

    print(tree2conlltags(nltk.ne_chunk(tags)))
    return JsonResponse(words,safe=False)
