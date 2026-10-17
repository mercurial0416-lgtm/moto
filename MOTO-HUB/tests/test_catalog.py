import json,pathlib,collections,re
base=pathlib.Path(__file__).resolve().parents[1]
a=json.loads((base/'app/src/main/assets/motorcycle_catalog.json').read_text('utf8'))['models']
assert len({x['brand'] for x in a})>=45
assert len(a)>=500
assert len({x['id'] for x in a})==len(a)
assert all(x['brand'] and x['model'] and isinstance(x['aliases'],list) for x in a)
for brand,name in [('Honda','Super Cub 110'),('Yamaha','YZF-R3'),('Kawasaki','ZX-4RR'),('KTM','390 Duke'),('BMW Motorrad','G 310 R'),('CFMOTO','450SR')]:
    assert any(x['brand']==brand and x['model']==name for x in a),(brand,name)
assert any('슈퍼커브' in x['aliases'] for x in a)
s=(base/'app/src/main/java/com/motohub/next/MainActivity.java').read_text('utf8')
assert 'showBikeCatalog' in s and 'pickCatalogYear' in s and 'catalogBrand' in s
hero=(base/'app/src/main/java/com/motohub/next/GarageHomeUi.java').read_text('utf8')
assert 'MotorcyclePhoto.show' in hero and 'garagePhotoFile' in hero
photos=(base/'app/src/main/java/com/motohub/next/MotorcyclePhoto.java').read_text('utf8')
assert 'wikimedia.org' in photos and 'LicenseShortName' in photos and 'matches(' in photos
print(f'PASS catalog: {len({x["brand"] for x in a})} brands, {len(a)} models, unique IDs, offline aliases, year-photo and manual-photo wiring')
