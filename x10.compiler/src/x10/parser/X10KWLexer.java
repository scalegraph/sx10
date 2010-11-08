/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */
/*****************************************************
 * WARNING!  THIS IS A GENERATED FILE.  DO NOT EDIT! *
 *****************************************************/

package x10.parser;

import lpg.runtime.*;
import java.util.*;

public class X10KWLexer extends X10KWLexerprs
{
    private char[] inputChars;
    private final int keywordKind[] = new int[61 + 1];

    public int[] getKeywordKinds() { return keywordKind; }

    public int lexer(int curtok, int lasttok)
    {
        int current_kind = getKind(inputChars[curtok]),
            act;

        for (act = tAction(START_STATE, current_kind);
             act > NUM_RULES && act < ACCEPT_ACTION;
             act = tAction(act, current_kind))
        {
            curtok++;
            current_kind = (curtok > lasttok
                                   ? X10KWLexersym.Char_EOF
                                   : getKind(inputChars[curtok]));
        }

        if (act > ERROR_ACTION)
        {
            curtok++;
            act -= ERROR_ACTION;
        }

        return keywordKind[act == ERROR_ACTION  || curtok <= lasttok ? 0 : act];
    }

    public void setInputChars(char[] inputChars) { this.inputChars = inputChars; }


    final static int tokenKind[] = new int[128];
    static
    {
        tokenKind['a'] = Char_a;
        tokenKind['b'] = Char_b;
        tokenKind['c'] = Char_c;
        tokenKind['d'] = Char_d;
        tokenKind['e'] = Char_e;
        tokenKind['f'] = Char_f;
        tokenKind['g'] = Char_g;
        tokenKind['h'] = Char_h;
        tokenKind['i'] = Char_i;
        tokenKind['j'] = Char_j;
        tokenKind['k'] = Char_k;
        tokenKind['l'] = Char_l;
        tokenKind['m'] = Char_m;
        tokenKind['n'] = Char_n;
        tokenKind['o'] = Char_o;
        tokenKind['p'] = Char_p;
        tokenKind['q'] = Char_q;
        tokenKind['r'] = Char_r;
        tokenKind['s'] = Char_s;
        tokenKind['t'] = Char_t;
        tokenKind['u'] = Char_u;
        tokenKind['v'] = Char_v;
        tokenKind['w'] = Char_w;
        tokenKind['x'] = Char_x;
        tokenKind['y'] = Char_y;
        tokenKind['z'] = Char_z;
    };

    final int getKind(char c)
    {
        return (c < 128 ? tokenKind[c] : 0);
    }


    public X10KWLexer(char[] inputChars, int identifierKind)
    {
        this.inputChars = inputChars;
        keywordKind[0] = identifierKind;

        //
        // Rule 1:  KeyWord ::= a b s t r a c t
        //
        
            keywordKind[1] = (X10Parsersym.TK_abstract);
      
    
        //
        // Rule 2:  KeyWord ::= a s
        //
        
            keywordKind[2] = (X10Parsersym.TK_as);
      
    
        //
        // Rule 3:  KeyWord ::= a s s e r t
        //
        
            keywordKind[3] = (X10Parsersym.TK_assert);
      
    
        //
        // Rule 4:  KeyWord ::= a s y n c
        //
        
            keywordKind[4] = (X10Parsersym.TK_async);
      
    
        //
        // Rule 5:  KeyWord ::= a t
        //
        
            keywordKind[5] = (X10Parsersym.TK_at);
      
    
        //
        // Rule 6:  KeyWord ::= a t e a c h
        //
        
            keywordKind[6] = (X10Parsersym.TK_ateach);
      
    
        //
        // Rule 7:  KeyWord ::= a t o m i c
        //
        
            keywordKind[7] = (X10Parsersym.TK_atomic);
      
    
        //
        // Rule 8:  KeyWord ::= b r e a k
        //
        
            keywordKind[8] = (X10Parsersym.TK_break);
      
    
        //
        // Rule 9:  KeyWord ::= c a s e
        //
        
            keywordKind[9] = (X10Parsersym.TK_case);
      
    
        //
        // Rule 10:  KeyWord ::= c a t c h
        //
        
            keywordKind[10] = (X10Parsersym.TK_catch);
      
    
        //
        // Rule 11:  KeyWord ::= c l a s s
        //
        
            keywordKind[11] = (X10Parsersym.TK_class);
      
    
        //
        // Rule 12:  KeyWord ::= c l o c k e d
        //
        
            keywordKind[12] = (X10Parsersym.TK_clocked);
      
    
        //
        // Rule 13:  KeyWord ::= c o n t i n u e
        //
        
            keywordKind[13] = (X10Parsersym.TK_continue);
      
    
        //
        // Rule 14:  KeyWord ::= d e f
        //
        
            keywordKind[14] = (X10Parsersym.TK_def);
      
    
        //
        // Rule 15:  KeyWord ::= d e f a u l t
        //
        
            keywordKind[15] = (X10Parsersym.TK_default);
      
    
        //
        // Rule 16:  KeyWord ::= d o
        //
        
            keywordKind[16] = (X10Parsersym.TK_do);
      
    
        //
        // Rule 17:  KeyWord ::= e l s e
        //
        
            keywordKind[17] = (X10Parsersym.TK_else);
      
    
        //
        // Rule 18:  KeyWord ::= e x t e n d s
        //
        
            keywordKind[18] = (X10Parsersym.TK_extends);
      
    
        //
        // Rule 19:  KeyWord ::= f a l s e
        //
        
            keywordKind[19] = (X10Parsersym.TK_false);
      
    
        //
        // Rule 20:  KeyWord ::= f i n a l
        //
        
            keywordKind[20] = (X10Parsersym.TK_final);
      
    
        //
        // Rule 21:  KeyWord ::= f i n a l l y
        //
        
            keywordKind[21] = (X10Parsersym.TK_finally);
      
    
        //
        // Rule 22:  KeyWord ::= f i n i s h
        //
        
            keywordKind[22] = (X10Parsersym.TK_finish);
      
    
        //
        // Rule 23:  KeyWord ::= f o r
        //
        
            keywordKind[23] = (X10Parsersym.TK_for);
      
    
        //
        // Rule 24:  KeyWord ::= g o t o
        //
        
            keywordKind[24] = (X10Parsersym.TK_goto);
      
    
        //
        // Rule 25:  KeyWord ::= h e r e
        //
        
            keywordKind[25] = (X10Parsersym.TK_here);
      
    
        //
        // Rule 26:  KeyWord ::= i f
        //
        
            keywordKind[26] = (X10Parsersym.TK_if);
      
    
        //
        // Rule 27:  KeyWord ::= i m p l e m e n t s
        //
        
            keywordKind[27] = (X10Parsersym.TK_implements);
      
    
        //
        // Rule 28:  KeyWord ::= i m p o r t
        //
        
            keywordKind[28] = (X10Parsersym.TK_import);
      
    
        //
        // Rule 29:  KeyWord ::= i n
        //
        
            keywordKind[29] = (X10Parsersym.TK_in);
      
    
        //
        // Rule 30:  KeyWord ::= i n s t a n c e o f
        //
        
            keywordKind[30] = (X10Parsersym.TK_instanceof);
      
    
        //
        // Rule 31:  KeyWord ::= i n t e r f a c e
        //
        
            keywordKind[31] = (X10Parsersym.TK_interface);
      
    
        //
        // Rule 32:  KeyWord ::= n a t i v e
        //
        
            keywordKind[32] = (X10Parsersym.TK_native);
      
    
        //
        // Rule 33:  KeyWord ::= n e w
        //
        
            keywordKind[33] = (X10Parsersym.TK_new);
      
    
        //
        // Rule 34:  KeyWord ::= n e x t
        //
        
            keywordKind[34] = (X10Parsersym.TK_next);
      
    
        //
        // Rule 35:  KeyWord ::= n u l l
        //
        
            keywordKind[35] = (X10Parsersym.TK_null);
      
    
        //
        // Rule 36:  KeyWord ::= o f f e r
        //
        
            keywordKind[36] = (X10Parsersym.TK_offer);
      
    
        //
        // Rule 37:  KeyWord ::= o f f e r s
        //
        
            keywordKind[37] = (X10Parsersym.TK_offers);
      
    
        //
        // Rule 38:  KeyWord ::= o p e r a t o r
        //
        
            keywordKind[38] = (X10Parsersym.TK_operator);
      
    
        //
        // Rule 39:  KeyWord ::= p a c k a g e
        //
        
            keywordKind[39] = (X10Parsersym.TK_package);
      
    
        //
        // Rule 40:  KeyWord ::= p r i v a t e
        //
        
            keywordKind[40] = (X10Parsersym.TK_private);
      
    
        //
        // Rule 41:  KeyWord ::= p r o p e r t y
        //
        
            keywordKind[41] = (X10Parsersym.TK_property);
      
    
        //
        // Rule 42:  KeyWord ::= p r o t e c t e d
        //
        
            keywordKind[42] = (X10Parsersym.TK_protected);
      
    
        //
        // Rule 43:  KeyWord ::= p u b l i c
        //
        
            keywordKind[43] = (X10Parsersym.TK_public);
      
    
        //
        // Rule 44:  KeyWord ::= r e s u m e
        //
        
            keywordKind[44] = (X10Parsersym.TK_resume);
      
    
        //
        // Rule 45:  KeyWord ::= r e t u r n
        //
        
            keywordKind[45] = (X10Parsersym.TK_return);
      
    
        //
        // Rule 46:  KeyWord ::= s e l f
        //
        
            keywordKind[46] = (X10Parsersym.TK_self);
      
    
        //
        // Rule 47:  KeyWord ::= s t a t i c
        //
        
            keywordKind[47] = (X10Parsersym.TK_static);
      
    
        //
        // Rule 48:  KeyWord ::= s t r u c t
        //
        
            keywordKind[48] = (X10Parsersym.TK_struct);
      
    
        //
        // Rule 49:  KeyWord ::= s u p e r
        //
        
            keywordKind[49] = (X10Parsersym.TK_super);
      
    
        //
        // Rule 50:  KeyWord ::= s w i t c h
        //
        
            keywordKind[50] = (X10Parsersym.TK_switch);
      
    
        //
        // Rule 51:  KeyWord ::= t h i s
        //
        
            keywordKind[51] = (X10Parsersym.TK_this);
      
    
        //
        // Rule 52:  KeyWord ::= t h r o w
        //
        
            keywordKind[52] = (X10Parsersym.TK_throw);
      
    
        //
        // Rule 53:  KeyWord ::= t r a n s i e n t
        //
        
            keywordKind[53] = (X10Parsersym.TK_transient);
      
    
        //
        // Rule 54:  KeyWord ::= t r u e
        //
        
            keywordKind[54] = (X10Parsersym.TK_true);
      
    
        //
        // Rule 55:  KeyWord ::= t r y
        //
        
            keywordKind[55] = (X10Parsersym.TK_try);
      
    
        //
        // Rule 56:  KeyWord ::= t y p e
        //
        
            keywordKind[56] = (X10Parsersym.TK_type);
      
    
        //
        // Rule 57:  KeyWord ::= v a l
        //
        
            keywordKind[57] = (X10Parsersym.TK_val);
      
    
        //
        // Rule 58:  KeyWord ::= v a r
        //
        
            keywordKind[58] = (X10Parsersym.TK_var);
      
    
        //
        // Rule 59:  KeyWord ::= w h e n
        //
        
            keywordKind[59] = (X10Parsersym.TK_when);
      
    
        //
        // Rule 60:  KeyWord ::= w h i l e
        //
        
            keywordKind[60] = (X10Parsersym.TK_while);
      
    
        //
        // Rule 61:  KeyWord ::= h a s Z e r o
        //
        
            keywordKind[61] = (X10Parsersym.TK_hasZero);
      
    
        for (int i = 0; i < keywordKind.length; i++)
        {
            if (keywordKind[i] == 0)
                keywordKind[i] = identifierKind;
        }
    }
}

