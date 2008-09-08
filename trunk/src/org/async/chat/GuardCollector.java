/*  Copyright(c) 2007-2008 Laurent A.V. Szyster
 *
 *  This library is free software; you can redistribute it and/or modify
 *  it under the terms of version 2 of the GNU General Public License as
 *  published by the Free Software Foundation.
 *  
 *   http://www.gnu.org/copyleft/gpl.html
 *  
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA
 *  
 */

package org.async.chat;



public class GuardCollector implements Collector {
    protected Collector _guarded;
    protected int _limit;
    public GuardCollector (Collector guarded, int limit) {
        _guarded = guarded;
        _limit = limit;
    }
    public void handleData(byte[] data) throws Throwable {
        _limit = _limit - data.length;
        if (_limit < 0) {
            return;
        }
        _guarded.handleData(data);
    }
    public boolean handleTerminator() throws Throwable {
        return _guarded.handleTerminator();
    }
}
