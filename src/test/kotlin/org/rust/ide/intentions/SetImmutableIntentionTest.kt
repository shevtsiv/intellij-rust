package org.rust.ide.intentions

class SetImmutableIntentionTest : RsIntentionTestBase(SetImmutableIntention()) {
    fun testSetMutableVariable() = doAvailableTest(
        """ fn main() { let var: &mut i3/*caret*/2 = 52; } """,
        """ fn main() { let var: &i3/*caret*/2 = 52; } """
    )

    fun testSetMutableParameter() = doAvailableTest(
        """ fn func(param: &mut i3/*caret*/2) {} """,
        """ fn func(param: &i3/*caret*/2) {} """
    )
}
