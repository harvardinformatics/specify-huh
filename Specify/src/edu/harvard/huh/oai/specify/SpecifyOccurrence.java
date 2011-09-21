/**
 * This class modeled after the class of a similar name in the
 * au.org.tern.ecoinformatics.oai.provider.model package by
 * Terrestrial Ecosystem Research Network.  Their copyright statement is included
 * below.  --mmk 2011-09-20
 * 
 * Copyright 2010 Terrestrial Ecosystem Research Network, licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or
 * agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.harvard.huh.oai.specify;

/**
 * Basic dataset model, of a sample dataset that has details that map nicely to an EML Dataset.
 * 
 * Intended as an example for developers building custom models of real data/metadata.
 * 
 * @author Vaughan Hobbs
 * 
 */
public class SpecifyOccurrence {

	private Long   id;
	private String catalogNumber;
	private String occurrenceDetails;
	private String occurrenceRemarks;
	private String recordNumber;
	private String recordedBy;
	private String individualId;
	private String individualCuunt;
	private String sex;
	private String lifeStage;
	private String reproductiveCondition;
	private String behavior;
	private String establishmentMeans;
	private String occurrenceStatus;
	private String preparations;
	private String disposition;
	private String otherCatalogNumbers;
	private String previousIdentifications;
	private String associatedMedia;
	private String associatedReferences;
	private String associatedOccurrences;
	private String associatedSequences;
	private String associatedTaxa;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCatalogNumber() {
		return catalogNumber;
	}
	public void setCatalogNumber(String catalogNumber) {
		this.catalogNumber = catalogNumber;
	}
	public String getOccurrenceDetails() {
		return occurrenceDetails;
	}
	public void setOccurrenceDetails(String occurrenceDetails) {
		this.occurrenceDetails = occurrenceDetails;
	}
	public String getOccurrenceRemarks() {
		return occurrenceRemarks;
	}
	public void setOccurrenceRemarks(String occurrenceRemarks) {
		this.occurrenceRemarks = occurrenceRemarks;
	}
	public String getRecordNumber() {
		return recordNumber;
	}
	public void setRecordNumber(String recordNumber) {
		this.recordNumber = recordNumber;
	}
	public String getRecordedBy() {
		return recordedBy;
	}
	public void setRecordedBy(String recordedBy) {
		this.recordedBy = recordedBy;
	}
	public String getIndividualId() {
		return individualId;
	}
	public void setIndividualId(String individualId) {
		this.individualId = individualId;
	}
	public String getIndividualCuunt() {
		return individualCuunt;
	}
	public void setIndividualCuunt(String individualCuunt) {
		this.individualCuunt = individualCuunt;
	}
	public String getSex() {
		return sex;
	}
	public void setSex(String sex) {
		this.sex = sex;
	}
	public String getLifeStage() {
		return lifeStage;
	}
	public void setLifeStage(String lifeStage) {
		this.lifeStage = lifeStage;
	}
	public String getReproductiveCondition() {
		return reproductiveCondition;
	}
	public void setReproductiveCondition(String reproductiveCondition) {
		this.reproductiveCondition = reproductiveCondition;
	}
	public String getBehavior() {
		return behavior;
	}
	public void setBehavior(String behavior) {
		this.behavior = behavior;
	}
	public String getEstablishmentMeans() {
		return establishmentMeans;
	}
	public void setEstablishmentMeans(String establishmentMeans) {
		this.establishmentMeans = establishmentMeans;
	}
	public String getOccurrenceStatus() {
		return occurrenceStatus;
	}
	public void setOccurrenceStatus(String occurrenceStatus) {
		this.occurrenceStatus = occurrenceStatus;
	}
	public String getPreparations() {
		return preparations;
	}
	public void setPreparations(String preparations) {
		this.preparations = preparations;
	}
	public String getDisposition() {
		return disposition;
	}
	public void setDisposition(String disposition) {
		this.disposition = disposition;
	}
	public String getOtherCatalogNumbers() {
		return otherCatalogNumbers;
	}
	public void setOtherCatalogNumbers(String otherCatalogNumbers) {
		this.otherCatalogNumbers = otherCatalogNumbers;
	}
	public String getPreviousIdentifications() {
		return previousIdentifications;
	}
	public void setPreviousIdentifications(String previousIdentifications) {
		this.previousIdentifications = previousIdentifications;
	}
	public String getAssociatedMedia() {
		return associatedMedia;
	}
	public void setAssociatedMedia(String associatedMedia) {
		this.associatedMedia = associatedMedia;
	}
	public String getAssociatedReferences() {
		return associatedReferences;
	}
	public void setAssociatedReferences(String associatedReferences) {
		this.associatedReferences = associatedReferences;
	}
	public String getAssociatedOccurrences() {
		return associatedOccurrences;
	}
	public void setAssociatedOccurrences(String associatedOccurrences) {
		this.associatedOccurrences = associatedOccurrences;
	}
	public String getAssociatedSequences() {
		return associatedSequences;
	}
	public void setAssociatedSequences(String associatedSequences) {
		this.associatedSequences = associatedSequences;
	}
	public String getAssociatedTaxa() {
		return associatedTaxa;
	}
	public void setAssociatedTaxa(String associatedTaxa) {
		this.associatedTaxa = associatedTaxa;
	}
}
