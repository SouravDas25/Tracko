# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models


# Create your models here.
class Category(models.Model):
    id = models.AutoField(primary_key=True)
    name = models.CharField(max_length=100)

    def __str__(self):
        return self.name


class Entity(models.Model):
    EntityType = (
        (0, 'Primary'),
        (1, 'Secondary'),
        (2, 'Ternary'),
    )
    id = models.AutoField(primary_key=True)
    name = models.CharField(max_length=100)
    domain = models.CharField(max_length=100,unique=True)
    logo = models.CharField(max_length=100)
    entity_type = models.IntegerField(choices=EntityType)
    data = models.CharField(max_length=250, default=None, blank=True, null=True)
    category = models.ForeignKey(Category)

    def __str__(self):
        return self.name



