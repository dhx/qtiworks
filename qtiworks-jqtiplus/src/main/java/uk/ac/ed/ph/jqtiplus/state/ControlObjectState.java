/* Copyright (c) 2012-2013, University of Edinburgh.
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
 * This software is derived from (and contains code from) QTITools and MathAssessEngine.
 * QTITools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.jqtiplus.state;

import uk.ac.ed.ph.jqtiplus.internal.util.ObjectUtilities;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

/**
 * FIXME: Document this type
 *
 * @author David McKain
 */
public abstract class ControlObjectState implements Serializable {

    private static final long serialVersionUID = 4027764553360833372L;

    private Date entryTime;
    private Date endTime;
    private Date exitTime;

    private Date durationIntervalStartTime;
    private long durationAccumulated;

    public void reset() {
        this.entryTime = null;
        this.endTime = null;
        this.exitTime = null;
        this.durationIntervalStartTime = null;
        this.durationAccumulated = 0L;
    }

    public final Date getEntryTime() {
        return ObjectUtilities.safeClone(entryTime);
    }

    public final void setEntryTime(final Date enteredTime) {
        this.entryTime = ObjectUtilities.safeClone(enteredTime);
    }

    public final boolean isEntered() {
        return entryTime!=null;
    }


    public final Date getEndTime() {
        return ObjectUtilities.safeClone(endTime);
    }

    public final void setEndTime(final Date endTime) {
        this.endTime = ObjectUtilities.safeClone(endTime);
    }

    public final boolean isEnded() {
        return endTime!=null;
    }


    public final Date getExitTime() {
        return ObjectUtilities.safeClone(exitTime);
    }

    public final void setExitTime(final Date exitTime) {
        this.exitTime = ObjectUtilities.safeClone(exitTime);
    }

    public final boolean isExited() {
        return exitTime!=null;
    }


    public final Date getDurationIntervalStartTime() {
        return durationIntervalStartTime;
    }

    public final void setDurationIntervalStartTime(final Date outTime) {
        this.durationIntervalStartTime = ObjectUtilities.safeClone(outTime);
    }


    public final long getDurationAccumulated() {
        return durationAccumulated;
    }

    public final void setDurationAccumulated(final long durationAccumulated) {
        this.durationAccumulated = durationAccumulated;
    }

    //----------------------------------------------------------------

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ControlObjectState)) {
            return false;
        }

        final ControlObjectState other = (ControlObjectState) obj;
        return ObjectUtilities.nullSafeEquals(entryTime, other.entryTime)
                && ObjectUtilities.nullSafeEquals(endTime, other.endTime)
                && ObjectUtilities.nullSafeEquals(exitTime, other.exitTime)
                && durationAccumulated==other.durationAccumulated
                && ObjectUtilities.nullSafeEquals(durationIntervalStartTime, other.durationIntervalStartTime);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] {
                entryTime,
                endTime,
                exitTime,
                durationAccumulated,
                durationIntervalStartTime,
        });
    }
}
