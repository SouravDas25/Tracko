# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.contrib import admin

# Register your models here.
from apis.models import Category, Entity

admin.site.register(Category)

admin.site.register(Entity)