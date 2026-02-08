import requests as rest_calls


def primary_logo(domain):
    r = rest_calls.get('https://logo.clearbit.com/' + domain)
    return r


def secondary_logo(domain):
    r = rest_calls.get('https://ui-avatars.com/api/' + domain, params={'name': domain})
    return r
