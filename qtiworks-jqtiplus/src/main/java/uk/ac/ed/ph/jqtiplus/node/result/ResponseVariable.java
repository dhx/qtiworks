/* Copyright (c) 2012, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.jqtiplus.node.result;

import uk.ac.ed.ph.jqtiplus.attribute.value.IdentifierMultipleAttribute;
import uk.ac.ed.ph.jqtiplus.group.item.CorrectResponseGroup;
import uk.ac.ed.ph.jqtiplus.group.result.CandidateResponseGroup;
import uk.ac.ed.ph.jqtiplus.node.item.CorrectResponse;
import uk.ac.ed.ph.jqtiplus.node.item.response.declaration.ResponseDeclaration;
import uk.ac.ed.ph.jqtiplus.node.shared.FieldValue;
import uk.ac.ed.ph.jqtiplus.types.Identifier;
import uk.ac.ed.ph.jqtiplus.value.Value;
import uk.ac.ed.ph.jqtiplus.xperimental.ToRefactor;

import java.util.List;

/**
 * Response variables are declared by response declarations.
 * 
 * @author Jonathon Hare
 */
public class ResponseVariable extends ItemVariable {

    private static final long serialVersionUID = 6478318320056351297L;

    /** Name of this class in xml schema. */
    public static final String QTI_CLASS_NAME = "responseVariable";

    /** Name of choiceSequence attribute in xml schema. */
    public static final String ATTR_CHOICE_SEQUENCE_NAME = "choiceSequence";

    public ResponseVariable(AbstractResult parent) {
        super(parent, QTI_CLASS_NAME);

        getAttributes().add(new IdentifierMultipleAttribute(this, ATTR_CHOICE_SEQUENCE_NAME, null, null, false));

        getNodeGroups().add(new CorrectResponseGroup(this));
        getNodeGroups().add(new CandidateResponseGroup(this));
    }

    /**
     * Creates new responseVariable from given responseDeclaration.
     * 
     * @param parent parent of created responseVariable
     * @param declaration Response declaration to use
     */
    @ToRefactor
    public ResponseVariable(AbstractResult parent, ResponseDeclaration declaration, Value value, List<Identifier> shuffledInteractionChoiceIdentifiers) {
        this(parent);

        if (declaration != null) {
            setIdentifier(declaration.getIdentifier().toVariableReferenceIdentifier());
            setBaseType(declaration.getBaseType());
            setCardinality(declaration.getCardinality());
            setCorrectResponse(declaration.getCorrectResponse());

            final CandidateResponse response = new CandidateResponse(this);
            response.getFieldValues().addAll(FieldValue.getValues(response, value));
            setCandidateResponse(response);

            if (shuffledInteractionChoiceIdentifiers != null) {
                final List<Identifier> choiceSequence = getChoiceSequence();
                choiceSequence.clear();
                choiceSequence.addAll(shuffledInteractionChoiceIdentifiers);
            }
        }
    }

    /**
     * Gets value of choiceSequence attribute.
     * 
     * @return value of choiceSequence attribute
     */
    public List<Identifier> getChoiceSequence() {
        return getAttributes().getIdentifierMultipleAttribute(ATTR_CHOICE_SEQUENCE_NAME).getValueAsList();
    }

    /**
     * Gets correctResponse child.
     * 
     * @return correctResponse child
     * @see #setCorrectResponse
     */
    public CorrectResponse getCorrectResponse() {
        return getNodeGroups().getCorrectResponseGroup().getCorrectResponse();
    }

    /**
     * Sets new correctResponse child.
     * 
     * @param correctResponse new correctResponse child
     * @see #getCorrectResponse
     */
    public void setCorrectResponse(CorrectResponse correctResponse) {
        getNodeGroups().getCorrectResponseGroup().setCorrectResponse(correctResponse);
    }

    /**
     * Gets candidateResponse child.
     * 
     * @return candidateResponse child
     * @see #setCandidateResponse
     */
    public CandidateResponse getCandidateResponse() {
        return getNodeGroups().getCandidateResponseGroup().getCandidateResponse();
    }

    /**
     * Sets new candidateResponse child.
     * 
     * @param candidateResponse new candidateResponse child
     * @see #getCandidateResponse
     */
    public void setCandidateResponse(CandidateResponse candidateResponse) {
        getNodeGroups().getCandidateResponseGroup().setCandidateResponse(candidateResponse);
    }

}