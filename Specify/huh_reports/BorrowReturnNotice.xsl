<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:math="http://exslt.org/math"
	xmlns:date="http://exslt.org/dates-and-times" xmlns:huh="http://edu.harvard/huh/specify/reports/datamodel">

	<xsl:variable name="nameOfContact">
		<xsl:if test="borrowReturnNotice/title != ''">
			<xsl:value-of select="borrowReturnNotice/title" />.&#x20;
		</xsl:if>
		<xsl:value-of select="borrowReturnNotice/nameOfContact" />,&#x20;
		<xsl:value-of select="borrowReturnNotice/jobTitle" />
	</xsl:variable>
	<xsl:variable name="institution">
		<xsl:value-of select="borrowReturnNotice/institution" />
		<xsl:if test="borrowReturnNotice/acronym != ''">
			- (
			<xsl:value-of select="borrowReturnNotice/acronym" />
			)
		</xsl:if>
	</xsl:variable>

	<xsl:variable name="address1" select="borrowReturnNotice/address1" />
	<xsl:variable name="address2" select="borrowReturnNotice/address2" />
	<xsl:variable name="cityStateZip">
		<xsl:if test="borrowReturnNotice/city != ''">
			<xsl:value-of select="borrowReturnNotice/city" />,
		</xsl:if>
		<xsl:if test="borrowReturnNotice/state != ''">
			<xsl:value-of select="borrowReturnNotice/state" />,
		</xsl:if>
		<xsl:value-of select="borrowReturnNotice/zip" />
	</xsl:variable>

	<xsl:variable name="country" select="borrowReturnNotice/country" />
	<xsl:variable name="nameOfShippedBy">
		<xsl:value-of select="borrowReturnNotice/nameOfShippedBy" />
	</xsl:variable>

	<xsl:variable name="dateSent" select="borrowReturnNotice/dateSent" />
	<xsl:variable name="numberOfPackages" select="borrowReturnNotice/numberOfPackages" />

	<xsl:variable name="borrowNumber" select="borrowReturnNotice/borrowNumber" />
	<xsl:variable name="currentDueDate" select="borrowReturnNotice/currentDueDate" />
	<xsl:variable name="dateClosed" select="borrowReturnNotice/dateClosed" />
	<xsl:variable name="herbarium" select="borrowReturnNotice/herbarium" />
	<xsl:variable name="invoiceNumber" select="borrowReturnNotice/invoiceNumber" />
	<xsl:variable name="isAcknowledged" select="borrowReturnNotice/isAcknowledged" />
	<xsl:variable name="isClosed" select="borrowReturnNotice/isClosed" />
	<xsl:variable name="isTheirRequest" select="borrowReturnNotice/isTheirRequest" />
	<xsl:variable name="isVisitor" select="borrowReturnNotice/isVisitor" />
	<xsl:variable name="originalDueDate" select="borrowReturnNotice/originalDueDate" />
	<xsl:variable name="purpose" select="borrowReturnNotice/purpose" />
	<xsl:variable name="receivedDate" select="borrowReturnNotice/receivedDate" />
	<xsl:variable name="remarks" select="borrowReturnNotice/remarks" />
	<xsl:variable name="userType" select="borrowReturnNotice/userType" />


	<xsl:variable name="forUseBy" select="borrowReturnNotice/forUseBy" />
	
	<xsl:variable name="generalCollectionsCount" select="borrowReturnNotice/generalCollectionsCount"/>
	<xsl:variable name="nonSpecimensCount" select="borrowReturnNotice/nonSpecimensCount"/>
	<xsl:variable name="typesCount" select="borrowReturnNotice/typesCount"/>

	<xsl:variable name="generalCollectionsReturnedCount" select="borrowReturnNotice/generalCollectionsReturnedCount"/>
	<xsl:variable name="nonSpecimensReturnedCount" select="borrowReturnNotice/nonSpecimensReturnedCount"/>
	<xsl:variable name="typesReturnedCount" select="borrowReturnNotice/typesReturnedCount"/>
	
	<xsl:variable name="generalCollectionsBalanceDueCount" select="borrowReturnNotice/generalCollectionsBalanceDueCount"/>
	<xsl:variable name="nonSpecimensBalanceDueCount" select="borrowReturnNotice/nonSpecimensBalanceDueCount"/>
	<xsl:variable name="typesBalanceDueCount" select="borrowReturnNotice/typesBalanceDueCount"/>
	
	<xsl:variable name="totalBorrowedSum" select="borrowReturnNotice/totalBorrowedSum"/>
	<xsl:variable name="totalReturnedSum" select="borrowReturnNotice/totalReturnedSum"/>
	<xsl:variable name="totalBalanceDueSum" select="borrowReturnNotice/totalBalanceDueSum"/>
	
	<xsl:variable name="isOurRequest">
		<xsl:value-of select="$isTheirRequest != true" />
	</xsl:variable>
	
	<xsl:template match="/">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
			<fo:layout-master-set>
				<fo:simple-page-master master-name="LoanReport"
					page-width="8.5in" page-height="11in">
					<fo:region-body margin-left=".70in" margin-right=".70in"
						margin-top=".50in" margin-bottom=".25in" />
					<fo:region-before extent="1.5in" display-align="before" />
					<fo:region-after extent="3in" display-align="after" />
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:page-sequence master-reference="LoanReport"
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
												<xsl:value-of select="borrowReturnNotice/reportsDir" />/logo.png
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
						BORROWED MATERIAL
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
						a return of borrowed herbarium specimens as indicated below. Upon arrival
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
					<fo:block border-bottom-style="solid" border-bottom-width="2pt" margin-bottom="6mm"
						space-before="24pt" border-top-style="solid" border-top-width="2pt">
						<fo:table text-align="left">
							<fo:table-column column-width="45mm" />
							<fo:table-column column-width="50mm"/>
							<fo:table-column column-width="45mm"/>
							<fo:table-column />
							<fo:table-body>
								<fo:table-row>
									<fo:table-cell>
										<fo:block>Loan Number:</fo:block>
								    </fo:table-cell>
								    <fo:table-cell>
										<fo:block>
											<xsl:value-of select="$invoiceNumber" />
										</fo:block>
									</fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
								</fo:table-row>
								<fo:table-row>
								    <fo:table-cell>
										<fo:block>Date received at HUH:</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block>
											<xsl:value-of select="$receivedDate" />
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
								<xsl:if test="$isOurRequest">
									<fo:table-row>
										<fo:table-cell>	
											<fo:block>
												Our request
											</fo:block>
										</fo:table-cell>
										<fo:table-cell><fo:block /></fo:table-cell>
										<fo:table-cell><fo:block /></fo:table-cell>
									</fo:table-row>
								</xsl:if>
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
								<fo:table-row>
									<fo:table-cell><fo:block /></fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
									<fo:table-cell><fo:block /></fo:table-cell>
								</fo:table-row>
								<fo:table-row>
									<fo:table-cell>
										<fo:block padding-top="4mm">Date Returned:</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block padding-top="4mm"><xsl:value-of select="$dateSent" /></fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block padding-top="4mm">Returned by:</fo:block>
									</fo:table-cell>
									<fo:table-cell>
										<fo:block padding-top="4mm"><xsl:value-of select="$nameOfShippedBy" /></fo:block>
									</fo:table-cell>
								</fo:table-row>
							</fo:table-body>
						</fo:table>
					</fo:block>

					<fo:block text-align="left" font-weight="bold">
						SHIPMENT HISTORY
					</fo:block>
					<fo:block padding-top="4mm">
						<fo:table border-bottom-style="solid" border-bottom-width="2pt">
						<fo:table-column column-width="40mm" />
						<fo:table-column column-width="15mm" />
						<fo:table-column column-width="15mm" />
						<fo:table-column column-width="15mm" />
						<fo:table-column column-width="15mm" />
						<fo:table-column column-width="40mm" />
						<fo:table-column />
						<fo:table-header>
							<fo:table-row>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt"
										space-before="12pt" font-weight="bold">Date returned</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt" font-weight="bold">Items</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt" font-weight="bold">Types</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt" font-weight="bold">Other</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt" font-weight="bold">Total</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt" font-weight="bold">Higher taxon</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt" font-weight="bold">Taxon</fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-header>
						
						<fo:table-body>
							<xsl:if test="count(borrowReturnNotice/borrowMaterialDesc) &gt; 0">
								<xsl:for-each select="borrowReturnNotice/borrowMaterialDesc">
									<fo:table-row>
										<fo:table-cell>
											<fo:block padding-bottom="4mm" padding-top="4mm" font-style="italic">
												Original Shipment
											</fo:block>
										</fo:table-cell>
										<fo:table-cell>
											<fo:block padding-bottom="4mm" padding-top="4mm">
												<xsl:choose>
													<xsl:when test="borrowedGeneralCollections != ''">
														<xsl:value-of select="borrowedGeneralCollections" />
													</xsl:when>
													<xsl:otherwise>0</xsl:otherwise>
												</xsl:choose>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell>
											<fo:block padding-bottom="4mm" padding-top="4mm">
												<xsl:choose>
													<xsl:when test="borrowedTypes != ''">
														<xsl:value-of select="borrowedTypes" />
													</xsl:when>
													<xsl:otherwise>0</xsl:otherwise>
												</xsl:choose>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell>
											<fo:block padding-bottom="4mm" padding-top="4mm">
												<xsl:choose>
													<xsl:when test="borrowedNonSpecimens != ''">
														<xsl:value-of select="borrowedNonSpecimens" />
													</xsl:when>
													<xsl:otherwise>0</xsl:otherwise>
												</xsl:choose>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell>
											<fo:block padding-bottom="4mm" padding-top="4mm">
												<xsl:choose>
													<xsl:when test="borrowedLineTotal != ''">
														<xsl:value-of select="borrowedLineTotal" />
													</xsl:when>
													<xsl:otherwise>0</xsl:otherwise>
												</xsl:choose>
											</fo:block>
										</fo:table-cell>
										<fo:table-cell>
											<fo:block padding-bottom="4mm" padding-top="4mm"><xsl:value-of select="higherTaxon" /></fo:block>
										</fo:table-cell>
										<fo:table-cell>
											<fo:block padding-bottom="4mm" padding-top="4mm"><xsl:value-of select="taxon" /></fo:block>
										</fo:table-cell>
									</fo:table-row>
									
									<xsl:if test="description != ''">
										<fo:table-row>
											<fo:table-cell number-columns-spanned="2">
												<fo:block />
											</fo:table-cell>
											<fo:table-cell number-columns-spanned="5" padding-left="4mm" padding-top="2mm" padding-bottom="2mm" text-align="right">
												<fo:block>
													<xsl:value-of select="description" />
												</fo:block>
											</fo:table-cell>
										</fo:table-row>
									</xsl:if>

									<xsl:if test="count(returnDesc) &gt; 0">
										<fo:table-row>
											<fo:table-cell>
												<fo:block font-style="italic">Returns</fo:block>
											</fo:table-cell>
											<fo:table-cell><fo:block /></fo:table-cell>
											<fo:table-cell><fo:block /></fo:table-cell>
											<fo:table-cell><fo:block /></fo:table-cell>
											<fo:table-cell><fo:block /></fo:table-cell>
											<fo:table-cell><fo:block /></fo:table-cell>
											<fo:table-cell><fo:block /></fo:table-cell>
										</fo:table-row>
										<xsl:for-each select="returnDesc">
											<fo:table-row>
											<fo:table-cell>
												<fo:block margin-left="4mm">
													<xsl:choose>
														<xsl:when test="dateReturned != ''">
															<xsl:value-of select="dateReturned"/>
														</xsl:when>
													</xsl:choose>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell>
												<fo:block>
													<xsl:choose>
														<xsl:when test="returnedGeneralCollections != ''">
															<xsl:value-of select="returnedGeneralCollections" />
														</xsl:when>
														<xsl:otherwise>0</xsl:otherwise>
													</xsl:choose>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell>
												<fo:block>
													<xsl:choose>
														<xsl:when test="returnedTypes != ''">
															<xsl:value-of select="returnedTypes" />
														</xsl:when>
														<xsl:otherwise>0</xsl:otherwise>
													</xsl:choose>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell>
												<fo:block>
													<xsl:choose>
														<xsl:when test="returnedNonSpecimens != ''">
															<xsl:value-of select="returnedNonSpecimens" />
														</xsl:when>
														<xsl:otherwise>0</xsl:otherwise>
													</xsl:choose>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell>
												<fo:block>
													<xsl:choose>
														<xsl:when test="returnedLineTotal != ''">
															<xsl:value-of select="returnedLineTotal" />
														</xsl:when>
														<xsl:otherwise>0</xsl:otherwise>
													</xsl:choose>
												</fo:block>
											</fo:table-cell>
											<fo:table-cell number-columns-spanned="2">
												<fo:block />
											</fo:table-cell>
											</fo:table-row>
										</xsl:for-each>
									</xsl:if>
								</xsl:for-each>
							</xsl:if>
						
						</fo:table-body>
						</fo:table>
					</fo:block> 
					<fo:block>
						<fo:table>
						<fo:table-column column-width="40mm" />
						<fo:table-column column-width="15mm" />
						<fo:table-column column-width="15mm" />
						<fo:table-column column-width="15mm" />
						<fo:table-column column-width="15mm" />
						<fo:table-column column-width="40mm" />
						<fo:table-column />
						<fo:table-body>
							<fo:table-row>
								<fo:table-cell>
									<fo:block padding-top="2mm">
										total balance due: 
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block padding-top="2mm">
										<xsl:choose>
											<xsl:when test="$generalCollectionsBalanceDueCount != ''">
												<xsl:value-of select="$generalCollectionsBalanceDueCount" />
											</xsl:when>
											<xsl:otherwise>0</xsl:otherwise>
										</xsl:choose>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block padding-top="2mm">
										<xsl:choose>
											<xsl:when test="$typesBalanceDueCount != ''">
												<xsl:value-of select="$typesBalanceDueCount" />
											</xsl:when>
											<xsl:otherwise>0</xsl:otherwise>
										</xsl:choose>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block padding-top="2mm">
										<xsl:choose>
											<xsl:when test="$nonSpecimensReturnedCount != ''">
												<xsl:value-of select="$nonSpecimensReturnedCount" />
											</xsl:when>
											<xsl:otherwise>0</xsl:otherwise>
										</xsl:choose>
									</fo:block>
								</fo:table-cell>
								<fo:table-cell number-columns-spanned="3">
									<fo:block padding-top="2mm">
										<xsl:choose>
											<xsl:when test="$totalBalanceDueSum != ''">
												<xsl:value-of select="$totalBalanceDueSum" />
											</xsl:when>
											<xsl:otherwise>0</xsl:otherwise>
										</xsl:choose>
									</fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-body>
						</fo:table>

						<!-- mmk: removed this because borrow remarks is labeled "Internal Remarks" -->
						<!-- in the Specify UI, so remarks should not be included in the report.    -->
						<!-- <fo:block-container space-before="24pt">
						<fo:block linefeed-treatment="preserve" white-space-collapse="false" white-space-treatment="preserve">
							<xsl:value-of select="$remarks" />
						</fo:block>
						</fo:block-container> -->

					</fo:block>
					<fo:block-container page-break-inside="avoid" space-before="10mm" space-after="2mm">
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
