/*
 * Copyright (c) Joachim Ansorg, mail@ansorg-it.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.lang.parser.builtin.varDef;

import com.ansorgit.plugins.bash.lang.LanguageBuiltins;
import com.ansorgit.plugins.bash.lang.parser.BashElementTypes;
import com.ansorgit.plugins.bash.lang.parser.BashPsiBuilder;
import com.ansorgit.plugins.bash.lang.parser.MockPsiBuilder;
import com.ansorgit.plugins.bash.lang.parser.MockPsiTest;
import com.ansorgit.plugins.bash.lang.parser.builtin.varDef.ExportCommand;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.tree.IElementType;
import org.junit.Test;

import java.util.List;

/**
 * @author jansorg
 */
public class ExportCommandTest extends MockPsiTest {
    MockFunction parserFunction = new MockFunction() {
        @Override
        public boolean apply(BashPsiBuilder psi) {
            return new ExportCommand().parseIfValid(psi).isParsedSuccessfully();
        }
    };

    MockFunction parserFunctionWithMarker = new MockFunction() {
        @Override
        public boolean apply(BashPsiBuilder psi) {
            return new ExportCommand().parseIfValid(psi).isParsedSuccessfully();
        }

        @Override
        public boolean postCheck(MockPsiBuilder mockBuilder) {
            List<Pair<MockPsiBuilder.MockMarker, IElementType>> markers = mockBuilder.getDoneMarkers();
            if (markers.isEmpty()) {
                return false;
            }

            //has to contain at least one variable def element marker
            for (Pair<MockPsiBuilder.MockMarker, IElementType> marker : markers) {
                if (marker.getSecond() == BashElementTypes.VAR_DEF_ELEMENT) {
                    return true;
                }
            }

            return false;
        }
    };

    @Test
    public void testBuiltin() {
        LanguageBuiltins.varDefCommands.contains("export");
    }

    @Test
    public void testParse() {
        //export a=1
        mockTest(parserFunctionWithMarker, Lists.newArrayList("export"), WORD, ASSIGNMENT_WORD, EQ, WORD);
        //export a
        mockTest(parserFunctionWithMarker, Lists.newArrayList("export"), WORD, WORD);
        //export a=1 b=2
        mockTest(parserFunctionWithMarker, Lists.newArrayList("export"),
                WORD, ASSIGNMENT_WORD, EQ, WORD, WHITESPACE, ASSIGNMENT_WORD, EQ, WORD);
    }

    @Test
    public void testComplicated() {
        //>out a=1 export a=1
        mockTest(parserFunction, Lists.newArrayList(">", "out", " ", "a", "=", "1", " ", "export"),
                GREATER_THAN, WORD, WHITESPACE, ASSIGNMENT_WORD, EQ, ARITH_NUMBER, WHITESPACE, WORD,
                WHITESPACE, ASSIGNMENT_WORD, EQ, ARITH_NUMBER);
    }

    @Test
    public void testArrayAssignment() throws Exception {
        //export a=(1 2 3)
        mockTest(parserFunction, Lists.newArrayList("export"), WORD, WORD, EQ, LEFT_PAREN, WORD, WHITESPACE, WORD, WHITESPACE, WORD, RIGHT_PAREN);

        //export a=(1 [10]=2 3)
        mockTest(parserFunction, Lists.newArrayList("export"), WORD, WORD, EQ, LEFT_PAREN, WORD, WHITESPACE, LEFT_SQUARE, ARITH_NUMBER, RIGHT_SQUARE, EQ, WORD, WHITESPACE, WORD, RIGHT_PAREN);
    }

    //issue 515
    @Test
    public void testDynamicSubshellVar() throws Exception {
        //export $a
        mockTest(parserFunction, Lists.newArrayList("export"), WORD, VARIABLE);
        //export ${a}
        mockTest(parserFunction, Lists.newArrayList("export"), WORD, DOLLAR, LEFT_CURLY, WORD, RIGHT_CURLY);

        //export $(a)
        mockTest(parserFunction, Lists.newArrayList("export"), WORD, DOLLAR, LEFT_PAREN, WORD, RIGHT_PAREN);

        //export $a=$b
        mockTest(parserFunction, Lists.newArrayList("export"), WORD, VARIABLE, EQ, VARIABLE);
    }
}
