<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:math="http://exslt.org/math"
	xmlns:date="http://exslt.org/dates-and-times" xmlns:huh="http://edu.harvard/huh/specify/reports/datamodel">

	<xsl:variable name="nameOfContact">
		<xsl:if test="shipmentNotice/title != ''">
			<xsl:value-of select="shipmentNotice/title" />
			.&#x20;
		</xsl:if>
		<xsl:value-of select="shipmentNotice/nameOfContact" />
		,&#x20;
		<xsl:value-of select="shipmentNotice/jobTitle" />
	</xsl:variable>
	<xsl:variable name="institution">
		<xsl:value-of select="shipmentNotice/institution" />
		<xsl:if test="shipmentNotice/acronym != ''">
			- (
			<xsl:value-of select="shipmentNotice/acronym" />
			)
		</xsl:if>
	</xsl:variable>

	<xsl:variable name="address1" select="shipmentNotice/address1" />
	<xsl:variable name="address2" select="shipmentNotice/address2" />
	<xsl:variable name="cityStateZip">
		<xsl:if test="shipmentNotice/city != ''">
			<xsl:value-of select="shipmentNotice/city" />
			,
		</xsl:if>
		<xsl:if test="shipmentNotice/state != ''">
			<xsl:value-of select="shipmentNotice/state" />
			,
		</xsl:if>
		<xsl:value-of select="shipmentNotice/zip" />
	</xsl:variable>

	<xsl:variable name="country" select="shipmentNotice/country" />
	<xsl:variable name="nameOfShippedBy">
		<xsl:value-of select="shipmentNotice/nameOfShippedBy" />
	</xsl:variable>

	<xsl:variable name="shipmentDate" select="shipmentNotice/shipmentDate" />
	<xsl:variable name="numberOfPackages" select="shipmentNotice/numberOfPackages" />
	<xsl:variable name="shipmentNumber" select="shipmentNotice/shipmentNumber" />
	<xsl:variable name="remarks" select="shipmentNotice/remarks" />

	<xsl:template match="/">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="ShipmentNotice"
					page-width="8.5in" page-height="11in">
					<fo:region-body margin-left=".70in" margin-right=".70in"
						margin-top=".50in" margin-bottom="1.5in" />
					<fo:region-before extent="1.5in" display-align="before" />
					<fo:region-after extent="3in" display-align="after" />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="ShipmentNotice"
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
												<xsl:value-of select="shipmentNotice/reportsDir" />/logo.png
											</xsl:attribute>
										</fo:external-graphic>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell column-number="3">
									<fo:block-container>
										<fo:block font="12pt Times New Roman" font-weight="bold">
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
						space-after="12pt" font-weight="bold" text-decoration="underline">
						SHIPPING
						NOTICE</fo:block>
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
						a shipment with the following contents.  Upon arrival
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
					<fo:block border-bottom-style="solid" border-bottom-width="2pt" margin-bottom=".25in" margin-top=".25in"
						space-before="24pt" border-top-style="solid" border-top-width="2pt">
						<fo:table text-align="left">
							<fo:table-column column-width="45mm" />
							<fo:table-column />
							<fo:table-body>
								<fo:table-row>
								    <fo:table-cell>
										<fo:block margin-bottom=".25in" margin-top=".25in">
											<xsl:value-of select="$remarks" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
								<fo:table-row  margin-bottom=".25in" margin-top=".25in">
									<fo:table-cell>
										<fo:block>HUH Shipment number:</fo:block>
								    </fo:table-cell>
								    <fo:table-cell>
										<fo:block>
											<xsl:value-of select="$shipmentNumber" />
											(Please use this number in
											all communications)
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
								<fo:table-row margin-bottom=".25in" margin-top=".25in">
								    <fo:table-cell>
										<fo:block>Date sent:</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:value-of select="$shipmentDate" />
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</fo:table-body>
						</fo:table>
					</fo:block>
					
					<fo:block-container
						space-after="10mm">
						<fo:block>Received the above in good order</fo:block>
						<fo:block space-before="18pt">
							<fo:leader leader-length="80mm" leader-pattern="rule" />
							<fo:leader leader-length="50mm" leader-pattern="rule"
								padding-left="5mm" />
						</fo:block>
						<fo:block font-size="8pt">
							<fo:inline>Signed</fo:inline>
							<fo:inline padding-left="78mm">Date</fo:inline>
						</fo:block>
					</fo:block-container>

				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>

</xsl:stylesheet>
