    <!--
	Osm2GpsMid will include ways/pois into the first tile level where the way/poi type's scale is at least the tile level's minScale
	If you zoom out far, GpsMid will only load the tiles of lower scale levels to save memory and performance.
	-->
	<tileScaleLevels>
		<tileScaleLevel level="3" minScale ="20" />
		<tileScaleLevel level="2" minScale ="18" />
		<tileScaleLevel level="1" minScale ="15" />
		<!-- lower scales will be included in tile level 0 -->
	</tileScaleLevels>	