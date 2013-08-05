<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:math="http://exslt.org/math"
	xmlns:date="http://exslt.org/dates-and-times" xmlns:huh="http://edu.harvard/huh/specify/reports/datamodel">

	<xsl:variable name="accessionNumber" select="reportAccession/accessionNumber" />
	<xsl:variable name="institution" select="reportAccession/institution" />
	<xsl:variable name="accessionDate" select="reportAccession/accessionDate" />
	<xsl:variable name="recipientName" select="reportAccession/recipientName" />
	<xsl:variable name="accessionType" select="reportAccession/accessionType" />
	<xsl:variable name="from" select="reportAccession/from" />
	<xsl:variable name="boxes" select="reportAccession/boxes" />
	<xsl:variable name="purpose" select="reportAccession/purpose" />
	
	<xsl:variable name="staffName" select="reportAccession/staffName" />
	<xsl:variable name="affiliation" select="reportAccession/affiliation" />

	<xsl:variable name="description" select="reportAccession/description" />
	
	<xsl:variable name="nonTypeCount" select="reportAccession/nonTypeCount" />
	<xsl:variable name="typeCount" select="reportAccession/typeCount" />
	<xsl:variable name="nonSpecimenCount" select="reportAccession/nonSpecimenCount" />
	<xsl:variable name="discardCount" select="reportAccession/discardCount" />
	<xsl:variable name="distributeCount" select="reportAccession/distributeCount" />
	<xsl:variable name="returnCount" select="reportAccession/returnCount" />
	<xsl:variable name="net" select="reportAccession/net" />
	<xsl:variable name="total" select="reportAccession/total" />
	
	<xsl:template match="/">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="AccessionReport"
					page-width="8.5in" page-height="11in">
					<fo:region-body margin-left=".70in" margin-right=".70in"
						margin-top=".50in" margin-bottom="1.5in" />
					<fo:region-before extent="1.5in" display-align="before" />
					<fo:region-after extent="3in" display-align="after" />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="AccessionReport"
				font="12pt Times New Roman">
				<fo:static-content flow-name="xsl-region-before">
					<fo:block text-align="right" margin-right=".35in"
						margin-top=".25in">
						<fo:page-number />
					</fo:block>
				</fo:static-content>
				
				<fo:flow flow-name="xsl-region-body" font="12pt Times New Roman">
					<fo:table margin-left="50mm">
						<fo:table-column column-number="1" column-width="35mm"/>
						<fo:table-column column-number="2" column-width="40mm"/>

						<fo:table-body>
								<fo:table-row>
									<fo:table-cell>
										<fo:block wrap-option="no-wrap">Accession number:</fo:block>
								    </fo:table-cell>
								    <fo:table-cell>
										<fo:block wrap-option="no-wrap">
											<xsl:value-of select="$accessionNumber" />
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
								<fo:table-row>
								    <fo:table-cell>
										<fo:block wrap-option="no-wrap">Local Unit:</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block wrap-option="no-wrap">
											<xsl:value-of select="$institution" />
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
								<fo:table-row>
									<fo:table-cell>
										<fo:block wrap-option="no-wrap">Date Accessioned:</fo:block>
								   </fo:table-cell>
								   <fo:table-cell>
										<fo:block wrap-option="no-wrap">
											<xsl:value-of select="$accessionDate" />
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
								<fo:table-row>
									<fo:table-cell>
										<fo:block wrap-option="no-wrap">Curatorial Staff:</fo:block>
									</fo:table-cell>
									<fo:table-cell>	
										<fo:block wrap-option="no-wrap">
											<xsl:value-of select="$recipientName" />
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
								<fo:table-row>
								    <fo:table-cell>
										<fo:block wrap-option="no-wrap">From:</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block wrap-option="no-wrap">
											<xsl:value-of select="$from" />
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
						</fo:table-body>
					</fo:table>

					<fo:block text-align="center" space-before="15pt" font="15pt Times New Roman"
						space-after="15pt" text-decoration="underline">
							<fo:inline text-decoration="underline">
								<xsl:choose>
									<xsl:when test="$accessionType = 'FieldWork'">
										Incoming Staff Collection
									</xsl:when>
									<xsl:when test="$accessionType = 'Gift'">
										Incoming Gift
									</xsl:when>
									<xsl:when test="$accessionType = 'Exchange'">
										Incoming Exchange
									</xsl:when>
									<xsl:when test="$accessionType = 'Purchase'">
										Purchase
									</xsl:when>
									<xsl:otherwise>Accession</xsl:otherwise>
								</xsl:choose>
							</fo:inline>
					</fo:block>

					<xsl:if test="$accessionType = 'FieldWork'">
						<fo:table text-align="start">
							<fo:table-column column-number="1" column-width="38mm"/>
							<fo:table-column column-number="2" column-width="78mm"/>
							<fo:table-column column-number="3" column-width="5mm"/>
							<fo:table-column column-number="4" column-width="22mm"/>
							<fo:table-column column-number="5" column-width="10mm"/>
							<fo:table-body>
								<fo:table-row>
									<fo:table-cell text-decoration="underline">
										<fo:block wrap-option="no-wrap">HUH Staff Member: </fo:block>
										</fo:table-cell>
									<fo:table-cell>
										<fo:block wrap-option="no-wrap"><xsl:value-of select="$staffName" /></fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block wrap-option="no-wrap"></fo:block>
									</fo:table-cell>
									<fo:table-cell text-decoration="underline">
										<fo:block wrap-option="no-wrap">Affiliation: </fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block wrap-option="no-wrap"><xsl:value-of select="$affiliation" /></fo:block>
									</fo:table-cell>
								</fo:table-row>
							</fo:table-body>
						</fo:table>
					</xsl:if>

					<xsl:if test="$accessionType != 'FieldWork'">
						<fo:block space-before="15pt" space-after="15pt" font="12pt Times New Roman">
							<fo:inline  text-decoration="underline">Number of boxes: </fo:inline>
							<xsl:value-of select="$boxes" />
						</fo:block>
						<fo:block space-before="15pt" space-after="15pt" font="12pt Times New Roman">
							<fo:inline text-decoration="underline">Purpose: </fo:inline>
							<xsl:value-of select="$purpose" />
						</fo:block>
					</xsl:if>

					<fo:block space-before="15pt" space-after="15pt" font="12pt Times New Roman">
						<fo:inline  text-decoration="underline">Description: </fo:inline>
						<xsl:value-of select="$description" />
					</fo:block>
					
					<fo:table space-before="15pt" font="10pt Times New Roman">
						<fo:table-column column-number="1" column-width="20mm"/>
						<fo:table-column column-number="2" column-width="20mm"/>
						<fo:table-column column-number="3" column-width="20mm"/>
						<fo:table-column column-number="4" column-width="20mm"/>
						<fo:table-column column-number="5" column-width="20mm"/>
						<fo:table-column column-number="6" column-width="20mm"/>
						<fo:table-column column-number="7" column-width="20mm"/>
						<fo:table-column column-number="8" column-width="20mm"/>
						<fo:table-column column-number="9" column-width="20mm"/>
						
						<fo:table-header>
							<fo:table-row text-align="center" font-weight="bold" display-align="center">
								<fo:table-cell><fo:block>Geography</fo:block></fo:table-cell>
								<fo:table-cell><fo:block>Non-Type Ct.*</fo:block></fo:table-cell>
								<fo:table-cell><fo:block>Type Ct.*</fo:block></fo:table-cell>
								<fo:table-cell><fo:block>Non-specimen Ct.*</fo:block></fo:table-cell>
								<fo:table-cell><fo:block>Discard Ct.*</fo:block></fo:table-cell>
								<fo:table-cell><fo:block>Distribute Ct.</fo:block></fo:table-cell>
								<fo:table-cell><fo:block>Return Ct.</fo:block></fo:table-cell>
								<fo:table-cell><fo:block>Total accessioned for HUH**</fo:block></fo:table-cell>
								<fo:table-cell><fo:block>Total accessioned ***</fo:block></fo:table-cell>
							</fo:table-row>
						</fo:table-header>
						<fo:table-body>
							<xsl:for-each select="reportAccession/region">
								<fo:table-row border-style="solid" text-align="center">
									<fo:table-cell padding-right="2mm">
										<fo:block>
											<xsl:value-of select="name" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell padding-right="2mm">
										<fo:block>
											<xsl:choose>
												<xsl:when test="nonTypeCount != ''">
													<xsl:value-of select="nonTypeCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="typeCount != ''">
													<xsl:value-of select="typeCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="nonSpecimenCount != ''">
													<xsl:value-of select="nonSpecimenCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="discardCount != ''">
													<xsl:value-of select="discardCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="distributeCount != ''">
													<xsl:value-of select="distributeCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="returnCount != ''">
													<xsl:value-of select="returnCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="net != ''">
													<xsl:value-of select="net" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="total != ''">
													<xsl:value-of select="total" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
							</xsl:for-each>
							<fo:table-row border-style="solid" text-align="center">
									<fo:table-cell padding-right="2mm">
										<fo:block>
											Total
										</fo:block>
									</fo:table-cell>
									<fo:table-cell padding-right="2mm">
										<fo:block>
											<xsl:choose>
												<xsl:when test="$nonTypeCount != ''">
													<xsl:value-of select="$nonTypeCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$typeCount != ''">
													<xsl:value-of select="$typeCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$nonSpecimenCount != ''">
													<xsl:value-of select="$nonSpecimenCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$discardCount != ''">
													<xsl:value-of select="$discardCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$distributeCount != ''">
													<xsl:value-of select="$distributeCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$returnCount != ''">
													<xsl:value-of select="$returnCount" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$net != ''">
													<xsl:value-of select="$net" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:choose>
												<xsl:when test="$total != ''">
													<xsl:value-of select="$total" />
												</xsl:when>
												<xsl:otherwise>0</xsl:otherwise>
											</xsl:choose>
										</fo:block>
									</fo:table-cell>
								</fo:table-row>
						</fo:table-body>
					</fo:table>
					<fo:block font-weight="bold">* includes numbers of discard ct., distribute ct., and return ct.</fo:block>
					<fo:block font-weight="bold">** sum of non-type ct., type ct., and non-specimen ct., minus distribute, discard, and return cts.</fo:block>
					<fo:block font-weight="bold">*** sum of nonytype ct., type ct., non-specimen ct.</fo:block>

				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>

</xsl:stylesheet>
