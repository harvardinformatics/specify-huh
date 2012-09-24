<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:math="http://exslt.org/math"
	xmlns:date="http://exslt.org/dates-and-times" xmlns:huh="http://edu.harvard/huh/specify/reports/datamodel">

	<xsl:variable name="nameOfContact">
		<xsl:if test="exchangeOutNotice/title != ''">
			<xsl:value-of select="exchangeOutNotice/title" />
			.&#x20;
		</xsl:if>
		<xsl:value-of select="exchangeOutNotice/nameOfContact" />
		,&#x20;
		<xsl:value-of select="exchangeOutNotice/jobTitle" />
	</xsl:variable>
	<xsl:variable name="institution">
		<xsl:value-of select="exchangeOutNotice/institution" />
		<xsl:if test="exchangeOutNotice/acronym != ''">
			- (
			<xsl:value-of select="exchangeOutNotice/acronym" />
			)
		</xsl:if>
	</xsl:variable>

	<xsl:variable name="address1" select="exchangeOutNotice/address1" />
	<xsl:variable name="address2" select="exchangeOutNotice/address2" />
	<xsl:variable name="cityStateZip">
		<xsl:if test="exchangeOutNotice/city != ''">
			<xsl:value-of select="exchangeOutNotice/city" />
			,
		</xsl:if>
		<xsl:if test="exchangeOutNotice/state != ''">
			<xsl:value-of select="exchangeOutNotice/state" />
			,
		</xsl:if>
		<xsl:value-of select="exchangeOutNotice/zip" />
	</xsl:variable>

	<xsl:variable name="country" select="exchangeOutNotice/country" />
	<xsl:variable name="nameOfShippedBy">
		<xsl:value-of select="exchangeOutNotice/nameOfShippedBy" />
	</xsl:variable>

	<xsl:variable name="dateSent" select="exchangeOutNotice/dateSent" />
	<xsl:variable name="numberOfPackages" select="exchangeOutNotice/numberOfPackages" />

	<xsl:variable name="descriptionOfMaterial"
		select="exchangeOutNotice/descriptionOfMaterial" />
	<xsl:variable name="exchangeDate" select="exchangeOutNotice/exchangeDate" />
	<xsl:variable name="exchangeNumber" select="exchangeOutNotice/exchangeNumber" />
	<xsl:variable name="forUseBy" select="exchangeOutNotice/forUseBy" />
	<xsl:variable name="herbarium" select="exchangeOutNotice/herbarium" />
	<xsl:variable name="isAcknowledged" select="exchangeOutNotice/isAcknowledged" />
	<xsl:variable name="isTheirRequest" select="exchangeOutNotice/isTheirRequest" />
	<xsl:variable name="purpose" select="exchangeOutNotice/purpose" />
	<xsl:variable name="remarks" select="exchangeOutNotice/remarks" />

	<xsl:variable name="generalCollectionsOutCount"
		select="exchangeOutNotice/generalCollectionsOutCount" />
	<xsl:variable name="nonSpecimensOutCount"
		select="exchangeOutNotice/nonSpecimensOutCount" />
	<xsl:variable name="typesOutCount" select="exchangeOutNotice/typesOutCount" />

	<xsl:variable name="generalCollectionsInCount"
		select="exchangeOutNotice/generalCollectionsInCount" />
	<xsl:variable name="nonSpecimensInCount" select="exchangeOutNotice/nonSpecimensInCount" />
	<xsl:variable name="typesInCount" select="exchangeOutNotice/typesInCount" />

	<xsl:variable name="generalCollectionsBalanceCount"
		select="exchangeOutNotice/generalCollectionsBalanceCount" />
	<xsl:variable name="nonSpecimensBalanceCount"
		select="exchangeOutNotice/nonSpecimensBalanceCount" />
	<xsl:variable name="typesBalanceCount" select="exchangeOutNotice/typesBalanceCount" />

	<xsl:variable name="totalExchangedInSum" select="exchangeOutNotice/totalExchangedInSum" />
	<xsl:variable name="totalExchangedOutSum"
		select="exchangeOutNotice/totalExchangedOutSum" />
	<xsl:variable name="totalBalanceSum" select="exchangeOutNotice/totalBalanceSum" />

	<xsl:variable name="isOurRequest">
		<xsl:value-of select="$isTheirRequest != true" />
	</xsl:variable>

	<xsl:template match="/">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="ExchangeOutNotice"
					page-width="8.5in" page-height="11in">
					<fo:region-body margin-left=".70in" margin-right=".70in"
						margin-top=".50in" margin-bottom="1.5in" />
					<fo:region-before extent="1.5in" display-align="before" />
					<fo:region-after extent="3in" display-align="after" />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="ExchangeOutNotice"
				font="12pt Times New Roman">
				<fo:static-content flow-name="xsl-region-before">
					<fo:block text-align="right" margin-right=".35in"
						margin-top=".25in">
						<fo:page-number />
					</fo:block>
				</fo:static-content>
				<fo:static-content flow-name="xsl-region-after">
					<fo:block text-align="center" margin-bottom=".25in"
						margin-right="1in" margin-left="1in">
						Herbarium of the Arnold Arboretum
						(A) -
						Farlow Herbarium (FH) - Gray
						Herbarium (GH) - Economic
						Herbarium of
						Oakes Ames (ECON) &amp;
						Orchid Herbarium of Oakes Ames
						(AMES) and the
						Herbarium of the New
						England Botanical Club (NEBC)
					</fo:block>
				</fo:static-content>
				<fo:flow flow-name="xsl-region-body" font="12pt Times New Roman">
					<fo:table>
						<fo:table-column column-width="15%" />
						<fo:table-column column-width="10%" />
						<fo:table-column column-width="60%" />
						<fo:table-column column-width="15%" />
						<fo:table-body>
							<fo:table-row>
								<fo:table-cell column-number="2">
									<fo:block>
										<fo:external-graphic content-width="36pt">
											<xsl:attribute name="src">
												<xsl:value-of select="exchangeOutNotice/reportsDir" />/logo.png
											</xsl:attribute>
										</fo:external-graphic>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell column-number="3">
									<fo:block-container>
										<fo:block font="12pt Times New Roman" font-weight="bold">
											The Harvard University Herbaria
										</fo:block>
										<fo:block>22 Divinity Avenue, Cambridge, Massachusetts 02138,
											USA
										</fo:block>
										<fo:block>
											<fo:inline padding-right="10mm">Tel. 617-495-2365
											</fo:inline>
											<fo:inline>Fax. 617-495-9484</fo:inline>
										</fo:block>
									</fo:block-container>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
					</fo:table>
					<fo:block text-align="center" space-before="12pt"
						space-after="12pt" font-weight="bold" text-decoration="underline">
						EXCHANGE
						SHIPPING
						NOTICE
					</fo:block>
					<fo:block-container space-before="12pt" height="29mm">
						<fo:block>
							<xsl:value-of select="$nameOfContact" />&#x20;
					</fo:block>
					<fo:block>
						<xsl:value-of select="$institution" />&#x20;
					</fo:block>
					<fo:block>
						<xsl:value-of select="$address1" />&#x20;
					</fo:block>
					<fo:block>
						<xsl:value-of select="$address2" />&#x20;
					</fo:block>
					<fo:block>
						<xsl:value-of select="$cityStateZip" />&#x20;
					</fo:block>
					<fo:block space-after="12pt">
						<xsl:value-of select="$country" />&#x20;
					</fo:block>
					</fo:block-container>
					<fo:block space-before="12pt" space-after="24pt">We are sending
						you
						an exchange of herbarium specimens as indicated below. Upon arrival
						of
						this shipment, please verify its contents and acknowledge by
						signing
						one copy and returning it to the Director at the above
						address. If
						you have any questions please contact
						huh-requests@oeb.harvard.edu.
					</fo:block>
					<fo:block margin-left="74mm">
						<fo:leader leader-length="100mm" leader-pattern="rule" />
						<fo:block margin-left="3mm">
							<xsl:value-of select="$nameOfShippedBy" />
							for the Director
						</fo:block>
					</fo:block>
					<fo:block border-bottom-style="solid" border-bottom-width="2pt"
						space-before="24pt" border-top-style="solid" border-top-width="2pt">
						<fo:table text-align="left">
							<fo:table-column column-width="45mm" />
							<fo:table-column column-width="50mm"/>
							<fo:table-column column-width="45mm"/>
							<fo:table-column />
							<fo:table-body>
								<fo:table-row>
									<fo:table-cell>
										<fo:block>
											(<xsl:value-of select="$herbarium" />)	Exchange number:
										</fo:block>
								    </fo:table-cell>
								    <fo:table-cell>
										<fo:block>																				
											<xsl:value-of select="$exchangeNumber" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
								</fo:table-row>
								<fo:table-row>
								    <fo:table-cell>
										<fo:block>Date sent:</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:value-of select="$exchangeDate" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
								</fo:table-row>
								<fo:table-row>
									<fo:table-cell>
										<fo:block>For use by:</fo:block>
								   </fo:table-cell>
								   <fo:table-cell>
										<fo:block>
											<xsl:value-of select="$forUseBy" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
								</fo:table-row>
								
								<fo:table-row>								
									<fo:table-cell>
										<fo:block>Number of packages:</fo:block>
									</fo:table-cell>
									<fo:table-cell>	
										<fo:block>
											<xsl:value-of select="$numberOfPackages" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
								</fo:table-row>
								
							</fo:table-body>
						</fo:table>
					</fo:block>

					<xsl:if test="count(exchangeOutNotice/outGeoBatchDesc) &gt; 0">
						<fo:block padding-top="4mm">
						<fo:table border-style="solid">
							<fo:table-column column-width="12%" />
							<fo:table-column column-width="12%" />
							<fo:table-column column-width="12%" />
							<fo:table-column column-width="32%" />
							<fo:table-column column-width="32%" />
							<fo:table-header>
								<fo:table-row border-style="solid">
									<fo:table-cell padding-right="2mm">
										<fo:block font-weight="bold">General Collections</fo:block>
									</fo:table-cell>
									<fo:table-cell padding-right="2mm">
										<fo:block font-weight="bold">Types</fo:block>
									</fo:table-cell>
									<fo:table-cell padding-right="2mm">
										<fo:block font-weight="bold">Non-specimens</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block font-weight="bold">Geography</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block font-weight="bold">Description</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</fo:table-header>
							<fo:table-body>
								<xsl:for-each select="exchangeOutNotice/outGeoBatchDesc">
									<fo:table-row>
										<fo:table-cell padding-right="2mm">
											<fo:block>
												<xsl:choose>
													<xsl:when test="generalCollectionsOut != ''">
														<xsl:value-of select="generalCollectionsOut" />
													</xsl:when>
													<xsl:otherwise>
														0
													</xsl:otherwise>
												</xsl:choose>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell padding-right="2mm">
											<fo:block>
												<xsl:choose>
													<xsl:when test="typesOut != ''">
														<xsl:value-of select="typesOut" />
													</xsl:when>
													<xsl:otherwise>
														0
													</xsl:otherwise>
												</xsl:choose>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell padding-right="2mm">
											<fo:block>
												<xsl:choose>
													<xsl:when test="nonSpecimensOut != ''">
														<xsl:value-of select="nonSpecimensOut" />
													</xsl:when>
													<xsl:otherwise>
														0
													</xsl:otherwise>
												</xsl:choose>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell padding-right="2mm">
											<fo:block>
												<xsl:value-of select="geography" />
											</fo:block>
										</fo:table-cell>
										<fo:table-cell>
											<fo:block>
												<xsl:value-of select="description" />
											</fo:block>
										</fo:table-cell>
									</fo:table-row>
								</xsl:for-each>
								<fo:table-row border-style="solid">
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$generalCollectionsOutCount != ''">
													<xsl:value-of select="$generalCollectionsOutCount" />
												</xsl:when>
												<xsl:otherwise>
													0
												</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$typesOutCount != ''">
													<xsl:value-of select="$typesOutCount" />
												</xsl:when>
												<xsl:otherwise>
													0
												</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$nonSpecimensOutCount != ''">
													<xsl:value-of select="$nonSpecimensOutCount" />
												</xsl:when>
												<xsl:otherwise>
													0
												</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											total:
										<xsl:choose>
											<xsl:when test="$totalExchangedOutSum != ''">
												<xsl:value-of select="$totalExchangedOutSum" />
											</xsl:when>
											<xsl:otherwise>
												0
											</xsl:otherwise>
										</xsl:choose>
									</fo:block>
								</fo:table-cell>
								</fo:table-row>
							</fo:table-body>
						</fo:table>
						</fo:block>
					</xsl:if>
					 
				<fo:block>
					<fo:block-container height="20mm" space-before="24pt">
						<fo:block linefeed-treatment="preserve" white-space-collapse="false" white-space-treatment="preserve"><xsl:value-of select="$descriptionOfMaterial" /></fo:block>
					</fo:block-container>
				</fo:block>
					
			</fo:flow>
		</fo:page-sequence>
	</fo:root>
</xsl:template>

	<xsl:template name="sum">
		<xsl:param name="nodes" />
		<xsl:param name="sum" select="0" />

		<xsl:variable name="curr" select="$nodes[1]" />

		<xsl:if test="$curr">
			<xsl:variable name="runningsum" select="$sum + $curr" />
			<xsl:call-template name="sum">
				<xsl:with-param name="nodes" select="$nodes[position() &gt; 1]" />
				<xsl:with-param name="sum" select="$runningsum" />
			</xsl:call-template>
		</xsl:if>

		<xsl:if test="not($curr)">
			<xsl:value-of select="$sum" />
		</xsl:if>

	</xsl:template>
	<xsl:template name="currShipment">
		<xsl:param name="nodes" />
		<xsl:param name="shipment" select="$nodes[1]" />
		<xsl:if test="$shipment">
			<xsl:variable name="curr" select="$nodes[2]" />
			<xsl:if test="$curr">
				<xsl:variable name="difference"
					select="date:difference($shipment/shipmentDate, $curr/shipmentDate)" />
				<xsl:choose>
					<xsl:when test="date:seconds($difference) &gt;= 0">
						<xsl:call-template name="currShipment">
							<xsl:with-param name="nodes" select="$nodes[position() &gt; 1]" />
							<xsl:with-param name="shipment" select="$shipment/shipmentDate" />
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<xsl:call-template name="currShipment">
							<xsl:with-param name="nodes" select="$nodes[position() &gt; 1]" />
							<xsl:with-param name="shipment" select="$curr/shipmentDate" />
						</xsl:call-template>
					</xsl:otherwise>
				</xsl:choose>

			</xsl:if>
			<xsl:if test="not($curr)">
				<xsl:value-of select="$shipment/shipmentDate" />
			</xsl:if>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
