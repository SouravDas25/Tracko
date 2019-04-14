from __future__ import unicode_literals

from django.http import JsonResponse



def getImage(request):
    if 'domain' in request.GET:
        domain = request.GET['domain']
    return domain