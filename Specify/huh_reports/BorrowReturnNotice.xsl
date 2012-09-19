<?xml version="1.0"?>

<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:math="http://exslt.org/math"
	xmlns:date="http://exslt.org/dates-and-times" xmlns:huh="http://edu.harvard/huh/specify/reports/datamodel">

	<xsl:variable name="nameOfContact">
		<xsl:if test="borrowReturnNotice/title != ''">
			<xsl:value-of select="borrowReturnNotice/title" />
			.&#x20;
		</xsl:if>
		<xsl:value-of select="borrowReturnNotice/nameOfContact" />
		,&#x20;
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
			<xsl:value-of select="borrowReturnNotice/city" />
			,
		</xsl:if>
		<xsl:if test="borrowReturnNotice/state != ''">
			<xsl:value-of select="borrowReturnNotice/state" />
			,
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
	<xsl:variable name="invoiceNumber" select="borrowReturnNotice/invoiceNumber" />
	<xsl:variable name="isClosed" select="borrowReturnNotice/isClosed" />
	<xsl:variable name="originalDueDate" select="borrowReturnNotice/originalDueDate" />
	<xsl:variable name="receivedDate" select="borrowReturnNotice/receivedDate" />
	<xsl:variable name="remarks" select="borrowReturnNotice/remarks" />
	<xsl:variable name="purpose" select="borrowReturnNotice/text1" />
	<xsl:variable name="localUnit" select="borrowReturnNotice/text2" />
	<xsl:variable name="userType" select="borrowReturnNotice/text3" />
	<xsl:variable name="isAcknowledged" select="borrowReturnNotice/yesNo1" />
	<xsl:variable name="isTheirRequest" select="borrowReturnNotice/yesNo2" />
	<xsl:variable name="isVisitor" select="borrowReturnNotice/yesNo3" />
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
						margin-top=".50in" margin-bottom="1.5in" />
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
						a return of borrowed of herbarium specimens as indicated below. Upon arrival
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
										<fo:block>Their Loan Number:</fo:block>
								    </fo:table-cell>
								    <fo:table-cell>
										<fo:block>
											<xsl:value-of select="$borrowNumber" />
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

					<fo:block padding-top="8mm">
						<fo:table border-bottom-style="solid" border-bottom-width="2pt">
						<fo:table-column column-width="10mm" />
						<fo:table-column column-width="60mm" />
						<fo:table-column column-width="25mm" />
						<fo:table-column />
						<fo:table-header>
							<fo:table-row>
								<fo:table-cell number-columns-spanned="2">
									<fo:block border-bottom-style="solid" border-bottom-width="2pt"
										space-before="12pt">DESCRIPTION OF SPECIMENS</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt"># Returned</fo:block>
								</fo:table-cell>
								<fo:table-cell>
									<fo:block border-bottom-style="solid" border-bottom-width="2pt"># Balance Due</fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-header>
						
						<fo:table-body>
						<fo:table-row>
						<fo:table-cell>
							<fo:block>
								<xsl:choose>
									<xsl:when test="$generalCollectionsCount != ''">
										<xsl:value-of select="$generalCollectionsCount" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:block>
						</fo:table-cell>
						<fo:table-cell>
							<fo:block>general collections</fo:block>
						</fo:table-cell>
						<fo:table-cell>
							<fo:block>
								<xsl:choose>
									<xsl:when test="$generalCollectionsReturnedCount != ''">
										<xsl:value-of select="$generalCollectionsReturnedCount" />
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
									<xsl:when test="$generalCollectionsBalanceDueCount != ''">
										<xsl:value-of select="$generalCollectionsBalanceDueCount" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:block>
						</fo:table-cell>
						</fo:table-row>
						
						<fo:table-row>
						<fo:table-cell>
							<fo:block>
								<xsl:choose>
									<xsl:when test="$typesCount != ''">
										<xsl:value-of select="$typesCount" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:block>
						</fo:table-cell>
						<fo:table-cell>
							<fo:block>types</fo:block>
						</fo:table-cell>
						<fo:table-cell>
							<fo:block>
								<xsl:choose>
									<xsl:when test="$typesReturnedCount != ''">
										<xsl:value-of select="$typesReturnedCount" />
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
									<xsl:when test="$typesBalanceDueCount != ''">
										<xsl:value-of select="$typesBalanceDueCount" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:block>
						</fo:table-cell>
						</fo:table-row>
						
						<fo:table-row>
						<fo:table-cell>
							<fo:block>
								<xsl:choose>
									<xsl:when test="$nonSpecimensCount != ''">
										<xsl:value-of select="$nonSpecimensCount" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:block>
						</fo:table-cell>
						<fo:table-cell>
							<fo:block>non-specimens</fo:block>
						</fo:table-cell>
						<fo:table-cell>
							<fo:block>
								<xsl:choose>
									<xsl:when test="$nonSpecimensReturnedCount != ''">
										<xsl:value-of select="$nonSpecimensReturnedCount" />
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
									<xsl:when test="$nonSpecimensBalanceDueCount != ''">
										<xsl:value-of select="$nonSpecimensBalanceDueCount" />
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
					<fo:block>
						<fo:table>
						<fo:table-column column-width="10mm" />
						<fo:table-column column-width="60mm" />
						<fo:table-column column-width="25mm" />
						<fo:table-column />
						<fo:table-body>
						<fo:table-row>
						<fo:table-cell>
							<fo:block>
								<xsl:choose>
									<xsl:when test="$totalBorrowedSum != ''">
										<xsl:value-of select="$totalBorrowedSum" />
									</xsl:when>
									<xsl:otherwise>
										0
									</xsl:otherwise>
								</xsl:choose>
							</fo:block>
						</fo:table-cell>
						<fo:table-cell>
							<fo:block>total</fo:block>
						</fo:table-cell>
						<fo:table-cell>
							<fo:block>
								<xsl:choose>
									<xsl:when test="$totalReturnedSum != ''">
										<xsl:value-of select="$totalReturnedSum" />
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
									<xsl:when test="$totalBalanceDueSum != ''">
										<xsl:value-of select="$totalBalanceDueSum" />
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
						<fo:block-container height="20mm" space-before="24pt">
						<fo:block linefeed-treatment="preserve" white-space-collapse="false" white-space-treatment="preserve"><xsl:value-of select="$remarks" /></fo:block>
						</fo:block-container>
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
					<xsl:if test="count(borrowReturnNotice/borrowMaterialDescs) &gt; 0">
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
							<xsl:for-each select="borrowReturnNotice/borrowMaterialDerscs">
								<fo:table-row border-style="solid">
									<fo:table-cell padding-right="2mm">
										<fo:block>
											<xsl:value-of select="higherTaxon" />
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
					<!-- <fo:block space-before="12pt" space-after="12pt">
						Total unbarcoded
						sheet count:
						<xsl:value-of select="$unbarcodedTotalCount" />
					</fo:block> -->
					</xsl:if>
					<xsl:if test="count(borrowReturnNotice/borrowMaterials) &gt; 0">
					<fo:block space-after="24pt">LIST OF BORROWED MATERIALS
					</fo:block>
					<fo:table border-style="solid">
						<fo:table-column column-width="15%" />
						<fo:table-column column-width="40%" />
						<fo:table-column column-width="10%" />
						<fo:table-column column-width="35%" />
						<fo:table-header>
							<fo:table-row border-style="solid">
								<fo:table-cell padding-right="5mm">
									<fo:block font-weight="bold">Barcode</fo:block>
								</fo:table-cell>
								<fo:table-cell padding-right="5mm">
									<fo:block font-weight="bold">Taxon(a)</fo:block>
								</fo:table-cell>
								<fo:table-cell padding-right="5mm">
									<fo:block font-weight="bold">Type</fo:block>
								</fo:table-cell>
								<fo:table-cell padding-right="5mm">
									<fo:block font-weight="bold">Collector, Number</fo:block>
								</fo:table-cell>
							</fo:table-row>
						</fo:table-header>
						<fo:table-body>
							<xsl:for-each select="borrowReturnNotice/borrowMaterial">
								<xsl:choose>
									<xsl:when test="count(borrowMaterial) &gt; 1">
										<fo:table-row>
											<fo:table-cell padding-left="5mm"
												padding-right="5mm" padding-top="5mm" padding-bottom="5mm"
												number-columns-spanned="4">
												<fo:table border-style="solid">
													<fo:table-body>
														<xsl:for-each select="borrowMaterial">
															<fo:table-row>
																<fo:table-cell padding-right="5mm">
																	<fo:block>
																		<xsl:value-of select="identifier" />
																	</fo:block>
																</fo:table-cell>
																<fo:table-cell padding-right="5mm">
																	<fo:block>
																		<xsl:value-of select="taxon" />
																	</fo:block>
																</fo:table-cell>
																<fo:table-cell padding-right="5mm">
																	<fo:block>
																		<xsl:value-of select="type" />
																	</fo:block>
																</fo:table-cell>
																<fo:table-cell padding-right="5mm">
																	<fo:block>
																		<xsl:value-of select="collectorName" /><xsl:if test="collectorNumber != ''">, 
																		<xsl:value-of select="collectorNumber" />
																		</xsl:if>
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
												<fo:table-cell padding-right="5mm">
													<fo:block>
														<xsl:value-of select="identifier" />
													</fo:block>
												</fo:table-cell>
												<fo:table-cell padding-right="5mm">
													<fo:block>
														<xsl:value-of select="taxon" />
													</fo:block>
												</fo:table-cell>
												<fo:table-cell padding-right="5mm">
													<fo:block>
														<xsl:value-of select="type" />
													</fo:block>
												</fo:table-cell>
												<fo:table-cell padding-right="5mm">
													<fo:block>
														<xsl:value-of select="collectorName" /><xsl:if test="collectorNumber != ''">, <xsl:value-of select="collectorNumber" />
														</xsl:if>
													</fo:block>
												</fo:table-cell>
											</fo:table-row>
										</xsl:for-each>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</fo:table-body>
					</fo:table>
					<!-- <fo:block space-before="12pt" space-after="12pt">
						Total items:
						<xsl:value-of select="$fragmentCount" />
						<xsl:if test="$preparationCount != $fragmentCount"> (<xsl:value-of select="$barcodedSpecimens" /> barcoded) on/in <xsl:value-of select="$preparationCount" /> sheets/packets
						<fo:block font-style="italic">Note: a rectanglular border surrounding multiple items indicates that they are on the same preparation.</fo:block></xsl:if>
					</fo:block> -->
					</xsl:if>
					<!-- <xsl:if test="loanInventory != ''">
					<fo:block space-before="24pt" space-after="12pt">LOAN INVENTORY
						(<xsl:value-of select="$loanNumber" />)</fo:block>
					</xsl:if> -->
						<!--
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
						## types</fo:block> -->
						<!-- <fo:block linefeed-treatment="preserve" white-space-collapse="false" white-space-treatment="preserve"><xsl:value-of select="$loanInventory" /></fo:block>  -->
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
