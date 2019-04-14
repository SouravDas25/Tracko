# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from django.db import models
from django.forms.models import model_to_dict


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
    domain = models.CharField(max_length=100, unique=True)
    logo = models.CharField(max_length=100)
    entity_type = models.IntegerField(choices=EntityType)
    data = models.CharField(max_length=250, default=None, blank=True, null=True)
    category = models.ForeignKey(Category)

    def __str__(self):
        return self.name

    def get_dict(self):
        d = model_to_dict(self)
        d['category'] = self.category.name
        d['entity_type'] = self.get_entity_type_display()
        return d


class Tag(models.Model):
    id = models.AutoField(primary_key=True)
    name = models.CharField(max_length=100)
    entity = models.ForeignKey(Entity)

    def __str__(self):
        return self.name, self.entity.name
