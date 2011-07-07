<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:math="http://exslt.org/math"
	xmlns:date="http://exslt.org/dates-and-times" xmlns:huh="http://edu.harvard/huh/specify/reports/datamodel">

	<xsl:variable name="nameOfContact">
		<xsl:value-of select="reportLoan/nameOfContact" />
	</xsl:variable>
	<xsl:variable name="institution">
		<xsl:value-of select="reportLoan/institution" />
		- (
		<xsl:value-of select="reportLoan/acronym" />
		)
	</xsl:variable>

	<xsl:variable name="address1" select="reportLoan/address1" />
	<xsl:variable name="address2" select="reportLoan/address2" />
	<xsl:variable name="cityStateZip">
		<xsl:value-of select="reportLoan/city" />
		,
		<xsl:value-of select="reportLoan/state" />
		,
		<xsl:value-of select="reportLoan/zip" />
	</xsl:variable>

	<xsl:variable name="country" select="reportLoan/country" />
	<xsl:variable name="nameOfShippedBy">
		<xsl:value-of select="reportLoan/nameOfShippedBy" />
	</xsl:variable>

	<xsl:variable name="loanNumber" select="reportLoan/loanNumber" />
	<xsl:variable name="dateSent" select="reportLoan/dateSent" />
	<xsl:variable name="dateDue" select="reportLoan/dateDue" />
	<xsl:variable name="forUseBy" select="reportLoan/forUseBy" />
	<xsl:variable name="numberOfPackages" select="reportLoan/numberOfPackages" />

	<xsl:variable name="generalCollections" select="reportLoan/generalCollectionCount" />
	<xsl:variable name="nonSpecimens" select="reportLoan/nonSpecimenCount" />
	<xsl:variable name="barcodedSpecimens" select="reportLoan/barcodedSpecimenCount" />
	<xsl:variable name="total" select="reportLoan/totalCount" />

	<xsl:variable name="description" select="reportLoan/description" />

	<xsl:template match="/">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="LoanReport"
					page-width="8.5in" page-height="11in">
					<fo:region-body margin-left=".70in" margin-right=".70in"
						margin-top=".50in" margin-bottom=".50in" />
					<fo:region-before extent="1.5in" display-align="before" />
					<fo:region-after extent="3in" display-align="after" />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="LoanReport"
				font-family="Times" font-size="12pt">
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
				<fo:flow flow-name="xsl-region-body">
					<fo:table>
						<fo:table-column column-width="10%" />
						<fo:table-column column-width="90%" />
						<fo:table-body>
							<fo:table-row>
								<fo:table-cell>
									<fo:block>
										<fo:external-graphic src="huh_reports/logo.png"
											content-width="36pt" />
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block-container>
										<fo:block font-family="Times" font-size="16pt"
											font-weight="bold">
											The Harvard University Herbaria</fo:block>
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
						space-after="12pt" font-weight="bold" text-decoration="underline">LOAN
						SHIPPING
						NOTICE</fo:block>
					<fo:block space-before="12pt">
						<xsl:value-of select="$nameOfContact" />
					</fo:block>
					<fo:block>
						<xsl:value-of select="$institution" />
					</fo:block>
					<fo:block>
						<xsl:value-of select="$address1" />
					</fo:block>
					<fo:block>
						<xsl:value-of select="$address2" />
					</fo:block>
					<fo:block>
						<xsl:value-of select="$cityStateZip" />
					</fo:block>
					<fo:block space-after="12pt">
						<xsl:value-of select="$country" />
					</fo:block>
					<fo:block space-before="12pt" space-after="36pt">We are sending
						you
						a loan of herbarium specimens as indicated below. Upon arrival
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
							<fo:table-column />
							<fo:table-body>
								<fo:table-row>
									<fo:table-cell>
										<fo:block>HUH loan number:</fo:block>
										<fo:block>Date sent:</fo:block>
										<fo:block>Date due:</fo:block>
										<fo:block>For use by:</fo:block>
										<fo:block>Number of packages:</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:value-of select="$loanNumber" />
											(Please use this number in
											all communications)
										</fo:block>
										<fo:block>
											<xsl:value-of select="$dateSent" />
										</fo:block>
										<fo:block>
											<xsl:value-of select="$dateDue" />
										</fo:block>
										<fo:block>
											<xsl:value-of select="$forUseBy" />
										</fo:block>
										<fo:block>
											<xsl:value-of select="$numberOfPackages" />
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</fo:table-body>
						</fo:table>
					</fo:block>
					<fo:block border-bottom-style="solid" border-bottom-width="2pt"
						space-before="12pt">DESCRIPTION OF SPECIMENS</fo:block>
					<fo:block border-bottom-style="solid" border-bottom-width="2pt"
						margin-right="95mm">
						<fo:block>
							<fo:inline padding-right="5mm">
								<xsl:choose>
									<xsl:when test="$generalCollections != ''">
										<xsl:value-of select="$generalCollections" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:inline>
							<fo:inline>general collections</fo:inline>
						</fo:block>
						<!-- Provide information about group(s), families, genera -->
						<fo:block>
							<fo:inline padding-right="5mm">
								<xsl:choose>
									<xsl:when test="$nonSpecimens != ''">
										<xsl:value-of select="$nonSpecimens" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:inline>
							<fo:inline>non-specimens</fo:inline>
						</fo:block>
						<fo:block>
							<fo:inline padding-right="5mm">
								<xsl:choose>
									<xsl:when test="$barcodedSpecimens != ''">
										<xsl:value-of select="$barcodedSpecimens" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:inline>
							<fo:inline>barcoded specimens (attached sheet)</fo:inline>
						</fo:block>
					</fo:block>
					<fo:block>
						<fo:block>
							<fo:inline padding-right="5mm">
								<xsl:choose>
									<xsl:when test="$total != ''">
										<xsl:value-of select="$total" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:inline>
							<fo:inline>total</fo:inline>
						</fo:block>
						<fo:block space-before="12pt">
							<xsl:value-of select="$description" />
						</fo:block>
					</fo:block>
					<fo:block-container space-before="24pt"
						space-after="10mm">
						<fo:block>Received the above in good order</fo:block>
						<fo:block space-before="24pt">
							<fo:leader leader-length="80mm" leader-pattern="rule" />
							<fo:leader leader-length="50mm" leader-pattern="rule"
								padding-left="5mm" />
						</fo:block>
						<fo:block font-size="8pt">
							<fo:inline>Signed</fo:inline>
							<fo:inline padding-left="78mm">Date</fo:inline>
						</fo:block>
					</fo:block-container>
					<fo:block space-after="24pt">LOTS OF UNBARCODED SPECIMENS
					</fo:block>
					<fo:table border-style="solid">
						<fo:table-column column-width="15%" />
						<fo:table-column column-width="15%" />
						<fo:table-column column-width="70%" />
						<fo:table-header>
							<fo:table-row border-style="solid">
								<fo:table-cell padding-right="2mm">
									<fo:block font-weight="bold">Sheet Count</fo:block>
								</fo:table-cell>
								<fo:table-cell padding-right="2mm">
									<fo:block font-weight="bold">Taxon</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block font-weight="bold">Description</fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-header>
						<fo:table-body>
							<xsl:for-each select="reportLoan/unbarcodedSpecimen">
								<fo:table-row border-style="solid">
									<fo:table-cell padding-right="2mm">
										<fo:block>
											<xsl:value-of select="sheetCount" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell padding-right="2mm">
										<fo:block>
											<xsl:value-of select="taxon" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:value-of select="description" />
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:for-each>
						</fo:table-body>
					</fo:table>
					<fo:block space-before="12pt" space-after="12pt">
						Total unbarcoded
						sheet count:
						<xsl:value-of select="$generalCollections" />
					</fo:block>
					<fo:block space-after="24pt">LIST OF BARCODED SPECIMENS
					</fo:block>
					<fo:table border-style="solid">
						<fo:table-header>
							<fo:table-row border-style="solid">
								<fo:table-cell padding-right="2mm">
									<fo:block font-weight="bold">Barcode</fo:block>
								</fo:table-cell>
								<fo:table-cell padding-right="2mm">
									<fo:block font-weight="bold">Taxon(a)</fo:block>
								</fo:table-cell>
								<fo:table-cell padding-right="2mm">
									<fo:block font-weight="bold">Type</fo:block>
								</fo:table-cell>
								<fo:table-cell padding-right="2mm">
									<fo:block font-weight="bold">Collector/Number</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block font-weight="bold">Geography</fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-header>
						<fo:table-body>
							<xsl:for-each select="reportLoan/barcodedSpecimen">
								<xsl:choose>
									<xsl:when test="count(barcodedItem) > 1">
										<fo:table-row>
											<fo:table-cell padding-left="5mm"
												padding-right="5mm" padding-top="5mm" padding-bottom="5mm"
												number-columns-spanned="5">
												<fo:table border-style="solid">
													<fo:table-body>
														<xsl:for-each select="barcodedItem">
															<fo:table-row>
																<fo:table-cell padding-right="2mm">
																	<fo:block>
																		*
																		<xsl:value-of select="identifier" />
																	</fo:block>
																</fo:table-cell>
																<fo:table-cell padding-right="2mm">
																	<fo:block>
																		<xsl:value-of select="taxon" />
																	</fo:block>
																</fo:table-cell>
																<fo:table-cell padding-right="2mm">
																	<fo:block>
																		<xsl:value-of select="type" />
																	</fo:block>
																</fo:table-cell>
																<fo:table-cell padding-right="2mm">
																	<fo:block>
																		<xsl:value-of select="collectorName" />
																		,
																		<xsl:value-of select="collectorNumber" />
																	</fo:block>
																</fo:table-cell>
																<fo:table-cell>
																	<fo:block>
																		<xsl:value-of select="region" />
																	</fo:block>
																</fo:table-cell>
															</fo:table-row>
														</xsl:for-each>
													</fo:table-body>
												</fo:table>
											</fo:table-cell>
										</fo:table-row>
									</xsl:when>
									<xsl:otherwise>
										<xsl:for-each select="barcodedItem">
											<fo:table-row>
												<fo:table-cell padding-right="2mm">
													<fo:block>
														<xsl:value-of select="identifier" />
													</fo:block>
												</fo:table-cell>
												<fo:table-cell padding-right="2mm">
													<fo:block>
														<xsl:value-of select="taxon" />
													</fo:block>
												</fo:table-cell>
												<fo:table-cell padding-right="2mm">
													<fo:block>
														<xsl:value-of select="type" />
													</fo:block>
												</fo:table-cell>
												<fo:table-cell padding-right="2mm">
													<fo:block>
														<xsl:value-of select="collectorName" />
														,
														<xsl:value-of select="collectorNumber" />
													</fo:block>
												</fo:table-cell>
												<fo:table-cell>
													<fo:block>
														<xsl:value-of select="region" />
													</fo:block>
												</fo:table-cell>
											</fo:table-row>
										</xsl:for-each>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</fo:table-body>
					</fo:table>
					<fo:block space-before="12pt" space-after="12pt">
						Total barcoded
						items:
						<xsl:value-of select="$barcodedSpecimens" />
					</fo:block>
					<fo:block space-before="12pt" space-after="12pt">LOAN INVENTORY
						(loan number fill in loan number)</fo:block>
					<fo:block>Name of Herbarium, eg Taylor Herbarium:</fo:block>
					<fo:block-container text-indent="2mm">
						<fo:block>genus, species number of specimens ex folder/sheet
							number
				</fo:block>
						<fo:block>genus, species number of specimens</fo:block>
						<fo:block>genus, species number of types ex folder/sheet number
						</fo:block>
					</fo:block-container>
					<fo:block>Total: ## specimens, including ## types</fo:block>
					<fo:block space-before="12pt">Name of Herbarium, eg General
						Collection:</fo:block>
					<fo:block-container text-indent="2mm">
						<fo:block>genus, species number of specimens</fo:block>
						<fo:block>genus, species number of specimens</fo:block>
						<fo:block>genus, species number of types</fo:block>
					</fo:block-container>
					<fo:block>Total: ## specimens, including ## types</fo:block>
					<fo:block space-before="12pt">Grand total: ## specimens,
						including
						## types</fo:block>
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
