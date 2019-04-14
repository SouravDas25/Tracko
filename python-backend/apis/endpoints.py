from __future__ import unicode_literals

from django.http import JsonResponse, HttpResponse

from services import LogoService


def get_image(request):
    domain = ""
    if 'domain' in request.GET:
        domain = request.GET['domain']
        r = LogoService.primary_logo(domain)
        # print(r.status_code)
        if r.status_code != 200:
            r = LogoService.secondary_logo(domain)
        return HttpResponse(r, content_type=r.headers.get('content-type'))
    return JsonResponse(domain, safe=False)
