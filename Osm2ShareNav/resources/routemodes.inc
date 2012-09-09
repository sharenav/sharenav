	<!--
	Osm2ShareNav will check ways for route access restrictions specified for the route Modes specified in the entry "useRouting" in the properties file
	e.g. useRouting = "motorcar, bicycle".
	If restrictionPermit is true, ways having tags matching the restrictionKey and restrictionValues become accessible for routing.
	If restrictionPermit is missing or false, ways having tags matching the restrictionKey and restrictionValues become inaccessible for routing.
	This contains also toll rules for each route mode. The last matching toll rule gives the actual toll state if multiple rules match enabling/disabling the toll.
	-->
	<routeModes>
		<routeMode modeName = "motorcar" maxPrepareMeters="500" maxInMeters="899" maxEstimationSpeed="100" applyTurnRestrictions = "true" mainStreetNet="true">
			<routeAccessRestriction restrictionKey = "motorcar" restrictionValues = "yes|designated" restrictionPermit = "true"/>
			<routeAccessRestriction restrictionKey = "motorcar" restrictionValues = "no|private"/>
			<routeAccessRestriction restrictionKey = "motor_vehicle" restrictionValues = "yes|designated" restrictionPermit = "true"/>
			<routeAccessRestriction restrictionKey = "motor_vehicle" restrictionValues = "no|private"/>
			<routeAccessRestriction restrictionKey = "access" restrictionValues = "no|private"/>
			<routeAccessRestriction restrictionKey = "vehicle" restrictionValues = "no|private"/>
			<!-- http://wiki.openstreetmap.org/wiki/WikiProject_Haiti -->
			<routeAccessRestriction restrictionKey = "bridge" restrictionValues = "collapsed"/>
			<routeAccessRestriction restrictionKey = "impassable" restrictionValues = "yes"/>
			<tollRule tollKey = "toll" tollValues = "yes|true" enableToll = "true" debugTollRule = "false"/>
		</routeMode>
		<routeMode modeName = "bicycle" maxPrepareMeters="75" maxInMeters="299" maxEstimationSpeed="17" bicycleOppositeExceptions = "true">
				<routeAccessRestriction restrictionKey = "bicycle" restrictionValues = "yes|designated" restrictionPermit = "true"/>
				<routeAccessRestriction restrictionKey = "cycleway" restrictionValues = "track" restrictionPermit = "true"/>
				<routeAccessRestriction restrictionKey = "bicycle" restrictionValues = "no|private"/>
				<routeAccessRestriction restrictionKey = "access" restrictionValues = "no|private"/>
				<routeAccessRestriction restrictionKey = "vehicle" restrictionValues = "no|private"/>
				<!-- http://wiki.openstreetmap.org/wiki/WikiProject_Haiti -->
				<routeAccessRestriction restrictionKey = "bridge" restrictionValues = "collapsed"/>
				<routeAccessRestriction restrictionKey = "impassable" restrictionValues = "yes"/>
				<tollRule tollKey = "toll" tollValues = "yes|true" enableToll = "true" debugTollRule = "false"/>
		</routeMode>
		<routeMode modeName = "foot" maxPrepareMeters="50" maxInMeters="125" maxEstimationSpeed="4" againstAllOneWays = "true">
			<routeAccessRestriction restrictionKey = "foot" restrictionValues = "yes|designated" restrictionPermit = "true"/>
			<routeAccessRestriction restrictionKey = "foot" restrictionValues = "no|private"/>
			<routeAccessRestriction restrictionKey = "access" restrictionValues = "no|private"/>
			<!-- http://wiki.openstreetmap.org/wiki/WikiProject_Haiti -->
			<routeAccessRestriction restrictionKey = "bridge" restrictionValues = "collapsed"/>
			<routeAccessRestriction restrictionKey = "impassable" restrictionValues = "yes"/>
			<tollRule tollKey = "toll" tollValues = "yes|true" enableToll = "true" debugTollRule = "false"/>
		</routeMode>
	</routeModes>