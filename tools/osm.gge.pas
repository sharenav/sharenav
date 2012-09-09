//Geoget 2
// to use with geoget, to be added as GeoGet/script/export/osm.gge.pas
// (todo: make a proper plugin out of this)

var
  counter: integer;
  name: string;

function ExportExtension: string;
begin
  result := 'OSM';
end;

function ExportDescription: string;
begin
  result := 'OSM data for ShareNav';
end;

function ExportHint: string;
begin
  result := 'OSM format for ShareNav (Osm2ShareNav). To be combined with regular OSM data with osmosis.';
end;

function ExportHeader: string;
begin
  Result := '<?xml version="1.0" encoding="utf-8"?>' + CRLF;
  Result := Result + '<osm version="0.5" generator="GeoGet OSM export for Osm2ShareNav">' + CRLF;
  counter := 1;
end;

function ExportFooter: string;
begin
  result := '</osm>';
end;

function ExportPoint: string;
var
  s: string;
  n: integer;
  r: string;
begin
  Result := '';
  if GC.IsListed and not GC.IsDisabled then
  begin
    Result := Result + ' <node id="-2' + IntToStr(counter) + '" visible="true" lat="' + GC.Lat + '" lon="' + GC.Lon +
    '" version="6" timestamp="' + formatdatetime('yyyy"-"mm"-"dd"T"hh":"nn":"ss"Z', GC.Hidden) +
    '">' + CRLF;
    counter := counter + 1;
    Result := Result + '  <tag k="name" v="' + GC.ID +'"/>' + CRLF;
    name := ReplaceString(GC.Name, '''', '&#39;');
    name := ReplaceString(name, '&', '&amp;');
    Result := Result + '  <tag k="note" v=''' + name+ ' (' + GC.Difficulty + '/' + GC.Terrain + ')'
        +  '''/>' + CRLF;
    Result := Result + '  <tag k="url" v="' + GC.URL +'"/>' + CRLF;
    Result := Result + '  <tag k="geocache" v="' + GC.CacheType +'"/>' + CRLF;
    Result := Result + ' </node>' + CRLF;
    end;
end;
