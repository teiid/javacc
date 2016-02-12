/* Copyright (c) 2006, Sun Microsystems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Heavily modified from the original form to produce asciidoc
 * 
 */

package org.javacc.jjdoc;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.javacc.parser.*;

@SuppressWarnings("nls")
public class HTMLGenerator
  extends TextGenerator
  implements Generator
{
  private TreeMap<String, Set<String>> prods = new TreeMap();
  StringWriter sw;
  
  private void println(String s)
  {
    print(s + "\n");
  }
  
  public void text(String s)
  {
    if (this.omit) {
      return;
    }
    print(escape(s));
  }
  
  private String escape(String s)
  {
	  return s;
/*    StringBuilder ss = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); i++)
    {
      char c = s.charAt(i);
      switch (c)
      {
      case '*': 
      case '+': 
      case '-': 
      case '[': 
      case ']': 
      case '{': 
      case '|': 
      case '}': 
        ss.append('\\').append(c);
        break;
      default: 
        ss.append(c);
      }
    }
    return ss.toString();*/
  }
  
  public void print(String s)
  {
    this.ostr.print(s);
  }
  
  public void documentStart()
  {
    this.sw = new StringWriter();
    this.ostr = new PrintWriter(this.sw);
    for (Iterator iter = JavaCCGlobals.bnfproductions.iterator(); iter.hasNext();)
    {
      NormalProduction np = (NormalProduction)iter.next();
      String label = np.getLhs();
      loadProps(np.getFirstToken());
      HashMap<String, String> hashMap = new HashMap();
      for (Map.Entry<Object, Object> entry : this.p.entrySet()) {
        hashMap.put((String)entry.getKey(), (String)entry.getValue());
      }
      if (!hashMap.containsKey("name")) {
        hashMap.put("name", label);
      }
      if (Boolean.valueOf((String)hashMap.get("unused")).booleanValue()) {
        iter.remove();
      } else {
        this.prodMap.put(label, hashMap);
      }
    }
  }
  
  private void loadProps(Token t)
  {
    this.p.clear();
    t = t.specialToken;
    if (t != null) {
      try
      {
        this.p.load(new StringReader(t.image));
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }
  }
  
  public void documentEnd()
  {
    this.ostr.close();
    PrintWriter p = create_output_stream();
    p.append("= BNF for SQL Grammar\n\n");
    p.append("* Main Entry Points\n");
    for (Map.Entry<String, Map<String, String>> prodEntry : this.prodMap.entrySet()) {
      if (Boolean.valueOf((String)((Map)prodEntry.getValue()).get("index")).booleanValue()) {
        p.append("** ").append(prodLink(prodEntry.getKey(), false)).append("\n");
      }
    }
    p.append("* <<Reserved Keywords, Reserved Keywords>>\n");
    p.append("* <<Non-Reserved Keywords, Non-Reserved Keywords>>\n");
    p.append("* <<Reserved Keywords For Future Use, Reserved Keywords For Future Use>>\n");
    p.append("* <<Tokens, Tokens>>\n");
    p.append("* <<Production Cross-Reference, Production Cross-Reference>>\n");
    p.append("* <<Productions, Productions>>\n");
    
    //reserved
    p.append("\n== Reserved Keywords\n");
    p.append("\n|===\n|Keyword |Usage\n");
    for (Map.Entry<String, String> entry : this.keywordLiteralMap.entrySet()) {
      if (!this.nonReserved.contains(entry.getKey())) {
        addKeyWordEntry(p, entry.getKey(), entry.getValue());
      }
    }
    p.append("\n|===\n");
    
    //non reserved
    p.append("\n== Non-Reserved Keywords\n");
    p.append("\n|===\n|Name |Usage\n");
    for (String keyword : this.nonReserved) {
      addKeyWordEntry(p, keyword, this.keywordLiteralMap.get(keyword));
    }
    p.append("\n|===\n");
    
    //reserved for future
    p.append("\n== Reserved Keywords For Future Use\n");
    p.append("\n|===\n");
    int i = 0;
    for (Object entry : this.keywordLiteralMap.entrySet())
    {
      Set<String> productions = (Set)this.tokenMap.get(((Map.Entry)entry).getKey());
      if ((productions.isEmpty()) && (!this.nonReserved.contains(((Map.Entry)entry).getKey())))
      {
        p.append("|");
        p.append((CharSequence)((Map.Entry)entry).getValue());
        i++;
        if (i % 3 == 0) {
          p.append("\n");
        }
      }
    }
    if (i % 3 != 0) {
    	p.append("\n");
    }
    p.append("|===\n");
    
    //tokens
    p.append("\n== Tokens\n");
    p.append("\n|===\n|Name |Definition |Usage\n");
    for (Object entry : this.tokenMap.entrySet())
    {
      String keyword = this.keywordLiteralMap.get(((Map.Entry)entry).getKey());
      if (keyword == null)
      {
        p.append("\n|").append("[[token_" + (String)((Map.Entry)entry).getKey() + "]]_").append(this.tokenNameMap.get(((Map.Entry)entry).getKey())).append("_");
        p.append("\n|").append(emitRE(this.tokenReMap.get(((Map.Entry)entry).getKey())));
        p.append("\n|");
        appendProductionList(p, (Set)((Map.Entry)entry).getValue());
        p.append("\n");
      }
    }
    p.append("\n|===\n");
    
    //cross-reference
    p.append("\n== Production Cross-Reference\n");
    p.append("\n|===\n|Name |Usage\n");
    for (Object entry : this.prods.entrySet())
    {
      p.append("\n|").append("[[usage_" + (String)((Map.Entry)entry).getKey() + "]]_<<" + ((Map.Entry)entry).getKey() + ", ").append((String)((Map)this.prodMap.get(((Map.Entry)entry).getKey())).get("name")).append(">>_");
      p.append("\n|");
      appendProductionList(p, (Set)((Map.Entry)entry).getValue());
      p.append("\n");
    }
    p.append("\n|===\n");
    p.append(this.sw.getBuffer().toString());
    p.close();
  }
  
  private void appendProductionList(PrintWriter p, Set<String> prods)
  {
    for (Iterator<String> iter = prods.iterator(); iter.hasNext();)
    {
      String label = iter.next();
      p.append("<<").append(label).append(",").append((String)((Map)this.prodMap.get(label)).get("name")).append(">>");
      if (iter.hasNext())
      {
        p.append(", ");
      }
    }
  }
  
  private void addKeyWordEntry(PrintWriter p, String keyword, String literal)
  {
    Set<String> productions = (Set)this.tokenMap.get(keyword);
    if (productions.isEmpty()) {
      return;
    }
    p.append("\n|[[token_" + keyword + "]]_").append(literal).append("_");
    p.append("\n|");
    appendProductionList(p, productions);
    p.append("\n");
  }
  
  boolean omit = false;
  private Map<String, Set<String>> tokenMap = new TreeMap();
  private Map<String, String> keywordLiteralMap = new TreeMap();
  private Map<String, RegularExpression> tokenReMap = new TreeMap();
  private Map<String, String> tokenNameMap = new TreeMap();
  private Set<String> nonReserved = new TreeSet();
  private Properties p = new Properties();
  private Map<String, Map<String, String>> prodMap = new TreeMap();
  
  public void specialTokens(String s) {}
  
  public void tokenStart(TokenProduction tp)
  {
    if ((tp.lexStates != null) && (tp.lexStates[0].equals("DEFAULT")) && (tp.kind == 0)) {
      for (Iterator it2 = tp.respecs.iterator(); it2.hasNext();)
      {
        RegExprSpec res = (RegExprSpec)it2.next();
        if ((res.rexp.label != null) && (!res.rexp.label.equals("")))
        {
          this.tokenMap.put(res.rexp.label, new TreeSet());
          if ((res.rexp instanceof RStringLiteral))
          {
            RStringLiteral rs = (RStringLiteral)res.rexp;
            if (Character.isLetter(rs.image.charAt(0)))
            {
              this.keywordLiteralMap.put(rs.label, rs.image.toUpperCase());
            }
            else
            {
              this.tokenNameMap.put(res.rexp.label, res.rexp.label.toLowerCase());
              this.tokenReMap.put(res.rexp.label, res.rexp);
            }
          }
          else
          {
            String name = res.rexp.label.toLowerCase();
            loadProps(tp.firstToken);
            this.tokenNameMap.put(res.rexp.label, this.p.getProperty("name", name));
            this.tokenReMap.put(res.rexp.label, res.rexp);
          }
        }
      }
    }
    this.omit = true;
  }
  
  private String emitRE(RegularExpression re)
  {
    String returnString = "";
    if ((re instanceof RCharacterList))
    {
      RCharacterList cl = (RCharacterList)re;
      if (cl.negated_list) {
        returnString = returnString + "~";
      }
      returnString = returnString + "\\[";
      for (Iterator it = cl.descriptors.iterator(); it.hasNext();)
      {
        Object o = it.next();
        if ((o instanceof SingleCharacter))
        {
          returnString = returnString + "\"";
          char[] s = { ((SingleCharacter)o).ch };
          returnString = returnString + JavaCCParserInternals.add_escapes(new String(s));
          returnString = returnString + "\"";
        }
        else if ((o instanceof CharacterRange))
        {
          returnString = returnString + "\"";
          char[] s = { ((CharacterRange)o).getLeft() };
          returnString = returnString + JavaCCParserInternals.add_escapes(new String(s));
          returnString = returnString + "\"\\-\"";
          s[0] = ((CharacterRange)o).getRight();
          returnString = returnString + JavaCCParserInternals.add_escapes(new String(s));
          returnString = returnString + "\"";
        }
        else
        {
          throw new AssertionError("Oops: unknown character list element type.");
        }
        if (it.hasNext()) {
          returnString = returnString + ",";
        }
      }
      returnString = returnString + "\\]";
    }
    else if ((re instanceof RChoice))
    {
      RChoice c = (RChoice)re;
      for (Iterator it = c.getChoices().iterator(); it.hasNext();)
      {
        RegularExpression sub = (RegularExpression)it.next();
        returnString = returnString + emitRE(sub);
        if (it.hasNext()) {
          returnString = returnString + " \\| ";
        }
      }
    }
    else if ((re instanceof REndOfFile))
    {
      returnString = returnString + "EOF";
    }
    else if ((re instanceof RJustName))
    {
      RJustName jn = (RJustName)re;
      returnString = returnString + tokenLink(jn.label);
    }
    else if ((re instanceof ROneOrMore))
    {
      ROneOrMore om = (ROneOrMore)re;
      returnString = returnString + "(";
      returnString = returnString + emitRE(om.regexpr);
      returnString = returnString + ")+";
    }
    else if ((re instanceof RSequence))
    {
      RSequence s = (RSequence)re;
      for (Iterator it = s.units.iterator(); it.hasNext();)
      {
        RegularExpression sub = (RegularExpression)it.next();
        boolean needParens = false;
        if ((sub instanceof RChoice)) {
          needParens = true;
        }
        if (needParens) {
          returnString = returnString + "(";
        }
        returnString = returnString + emitRE(sub);
        if (needParens) {
          returnString = returnString + ")";
        }
        if (it.hasNext()) {
          returnString = returnString + " ";
        }
      }
    }
    else if ((re instanceof RStringLiteral))
    {
      RStringLiteral sl = (RStringLiteral)re;
      returnString = returnString + "\"" + escape(JavaCCParserInternals.add_escapes(sl.image)) + "\"";
    }
    else if ((re instanceof RZeroOrMore))
    {
      RZeroOrMore zm = (RZeroOrMore)re;
      returnString = returnString + "(";
      returnString = returnString + emitRE(zm.regexpr);
      returnString = returnString + ")*";
    }
    else if ((re instanceof RZeroOrOne))
    {
      RZeroOrOne zo = (RZeroOrOne)re;
      returnString = returnString + "(";
      returnString = returnString + emitRE(zo.regexpr);
      returnString = returnString + ")?";
    }
    else if ((re instanceof RRepetitionRange))
    {
      RRepetitionRange zo = (RRepetitionRange)re;
      returnString = returnString + "(";
      returnString = returnString + emitRE(zo.regexpr);
      returnString = returnString + ")";
      returnString = returnString + "\\{";
      if (zo.hasMax)
      {
        returnString = returnString + zo.min;
        returnString = returnString + ",";
        returnString = returnString + zo.max;
      }
      else
      {
        returnString = returnString + zo.min;
      }
      returnString = returnString + "\\}";
    }
    else
    {
      throw new AssertionError("Oops: Unknown regular expression type.");
    }
    return returnString;
  }
  
  public void tokenEnd(TokenProduction tp)
  {
    this.omit = false;
  }
  
  public void nonterminalsStart()
  {
    println("\n== Productions\n");
  }
  
  String production = null;
  
  public void nonterminalsEnd() {}
  
  public void tokensStart() {}
  
  public void tokensEnd() {}
  
  public void javacode(JavaCodeProduction jp) {}
  
  public void productionStart(NormalProduction np)
  {
    this.production = np.getLhs();
    print("\n=== [[" + np.getLhs() + "]]_<<usage_" + np.getLhs() + ", "+ (String)((Map)this.prodMap.get(np.getLhs())).get("name") + ">>_ ::= \n");
  }
  
  public void productionEnd(NormalProduction np)
  {
    Map<String, String> vals = (Map)this.prodMap.get(np.getLhs());
    String description = vals.get("description");
    if (description != null) {
      println("\n\n" + description);
    }
    String example = vals.get("example");
    if (example != null) {
      println("\n\nExample:\n" + example);
    }
    println("\n'''\n");
  }
  
  public void expansionStart(Expansion e, boolean first)
  {
    print("\n* ");
  }
  
  public void expansionEnd(Expansion e, boolean first)
  {
    print("\n");
  }
  
  public void nonTerminalStart(NonTerminal nt)
  {
    Set<String> vals = (Set)this.prods.get(nt.getName());
    if (vals == null)
    {
      vals = new TreeSet();
      this.prods.put(nt.getName(), vals);
    }
    vals.add(this.production);
    print(prodLink(nt.getName(), true));
    this.omit = true;
  }
  
  private String prodLink(String label, boolean enclose)
  {
    String result = "<<" + label + "," + (String)((Map)this.prodMap.get(label)).get("name") + ">>";
    if (enclose) {
      result = "<" + result + ">";
    }
    return result;
  }
  
  public void nonTerminalEnd(NonTerminal nt)
  {
    this.omit = false;
  }
  
  public void reStart(RegularExpression r)
  {
    if (!(r instanceof RJustName)) {
      return;
    }
    Set<String> vals = (Set)this.tokenMap.get(r.label);
    if (vals != null) {
      vals.add(this.production);
    }
    if ("nonReserved".equals(this.production)) {
      this.nonReserved.add(r.label);
    }
    print(tokenLink(r.label));
    this.omit = true;
  }
  
  private String tokenLink(String tokenLabel)
  {
    String keyword = this.keywordLiteralMap.get(tokenLabel);
    String label = tokenLabel;
    StringBuffer sb = new StringBuffer();
    if (keyword != null)
    {
      label = keyword;
    }
    else
    {
      label = this.tokenNameMap.get(tokenLabel);
      if (label == null) {
        throw new AssertionError();
      }
      sb.append("<");
    }
    sb.append("<<token_" + tokenLabel);
    sb.append("," + label + ">>");
    if (keyword == null) {
      sb.append(">");
    }
    return sb.toString();
  }
  
  public void reEnd(RegularExpression r)
  {
    this.omit = false;
  }
  
    @Override
	public void handleTokenProduction(TokenProduction tp) {
    	String token = JJDoc.getStandardTokenProductionText(tp);
        if (!token.equals("")) {
            tokenStart(tp);
            text(token);
            tokenEnd(tp);
        }
	}
}
