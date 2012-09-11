#!/bin/sh

# create translations for Osm2ShareNav
msgfmt --properties-input -r Translations -l fi -d . --java2  resources/Translations_fi.properties
msgfmt --properties-input -r Translations -l cs -d . --java2  resources/Translations_cs.properties
