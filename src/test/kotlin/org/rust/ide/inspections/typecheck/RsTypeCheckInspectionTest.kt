/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.typecheck

import org.rust.ExpandMacros
import org.rust.ProjectDescriptor
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.ide.inspections.RsInspectionsTestBase
import org.rust.ide.inspections.RsTypeCheckInspection
import org.rust.lang.core.macros.MacroExpansionScope

class RsTypeCheckInspectionTest : RsInspectionsTestBase(RsTypeCheckInspection()) {
    fun `test type mismatch E0308 primitive`() = checkByText("""
        fn main () {
            let _: u8 = <error>1u16</error>;
        }
    """)

    fun `test typecheck in constant`() = checkByText("""
        const A: u8 = <error>1u16</error>;
    """)

    fun `test typecheck in array size`() = checkByText("""
        const A: [u8; <error>1u8</error>] = [0];
    """)

    fun `test typecheck in enum variant discriminant`() = checkByText("""
        enum Foo { BAR = <error>1u8</error> }
    """)

    fun `test type mismatch E0308 coerce ptr mutability`() = checkByText("""
        fn fn_const(p: *const u8) { }
        fn fn_mut(p: *mut u8) { }

        fn main () {
            let mut ptr_const: *const u8;
            let mut ptr_mut: *mut u8;

            fn_const(ptr_const);
            fn_const(ptr_mut);
            fn_mut(<error>ptr_const</error>);
            fn_mut(ptr_mut);

            ptr_const = ptr_mut;
            ptr_mut = <error>ptr_const</error>;
        }
    """)

    fun `test type mismatch E0308 coerce reference to ptr`() = checkByText("""
        fn fn_const(p: *const u8) { }
        fn fn_mut(p: *mut u8) { }

        fn main () {
            let const_u8 = &1u8;
            let mut_u8 = &mut 1u8;
            fn_const(const_u8);
            fn_const(mut_u8);
            fn_mut(<error>const_u8</error>);
            fn_mut(mut_u8);
        }
    """)

    fun `test type mismatch E0308 struct`() = checkByText("""
        struct X; struct Y;
        fn main () {
            let _: X = <error>Y</error>;
        }
    """)

    fun `test type mismatch E0308 tuple`() = checkByText("""
        fn main () {
            let _: (u8, ) = (<error>1u16</error>, );
            let _: (u8, ) = <error>(1, 1)</error>;
        }
    """)

    // TODO error should be more local
    fun `test type mismatch E0308 array`() = checkByText("""
        fn main () {
            let _: [u8; 1] = <error>[1u16]</error>;
        }
    """)

    fun `test type mismatch E0308 array size`() = checkByText("""
        fn main () {
            let _: [u8; 1] = <error>[1, 2]</error>;
        }
    """)

    fun `test type mismatch E0308 struct field`() = checkByText("""
        struct S { f: u8 }
        fn main () {
            S { f: <error>1u16</error> };
        }
    """)

    fun `test type mismatch E0308 struct shorthand`() = checkByText("""
        struct S { f: u8 }
        fn main () {
            let f = "42";
            S { <error>f</error> };
        }
    """)

    fun `test type mismatch E0308 function parameter`() = checkByText("""
        fn foo(_: u8) {}
        fn main () {
            foo(<error>1u16</error>)
        }
    """)

    fun `test type mismatch E0308 unconstrained integer`() = checkByText("""
        struct S;
        fn main () {
            let mut a = 0;
            a = <error>S</error>;
        }
    """)

    // issue #1753
    fun `test no type mismatch E0308 for multiple impls of the same trait`() = checkByText("""
        pub trait From<T> { fn from(_: T) -> Self; }
        struct A; struct B; struct C;
        impl From<B> for A { fn from(_: B) -> A { unimplemented!() } }
        impl From<C> for A { fn from(_: C) -> A { unimplemented!() } }
        fn main() {
            A::from(C);
        }
    """)

    // issue #1790
    fun `test not type mismatch E0308 with unknown size array`() = checkByText("""
        struct Foo {
            v: [usize; COUNT]
        }

        fn main() {
            let x = Foo { v: [10, 20] };
        }
    """)

    // issue 1753
    fun `test no type mismatch E0308 on struct argument reference coercion`() = checkByText("""
        #[lang = "deref"]
        trait Deref { type Target; }

        struct Wrapper<T>(T);
        struct RefWrapper<'a, T : 'a>(&'a T);

        impl<T> Deref for Wrapper<T> { type Target = T; }

        fn foo(w: &Wrapper<u32>) {
            let _: RefWrapper<u32> = RefWrapper(w);
        }
    """)

    // issue 2670
    fun `test no type mismatch E0308 when matching with string literal`() = checkByText("""
        fn main() {
            match "" {
                "" => {}
                _ => {}
            }
        }
    """)

    // https://github.com/intellij-rust/intellij-rust/issues/2482
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test issue 2482`() = checkByText("""
        fn main() {
            let string: String = "string".to_owned();
        }
    """)

    // https://github.com/intellij-rust/intellij-rust/issues/2460
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test issue 2460`() = checkByText("""
        fn f64compare(x: &f64, y: &f64) -> ::std::cmp::Ordering {
            x.partial_cmp(y).unwrap()
        }
    """)

    /** Issue [2713](https://github.com/intellij-rust/intellij-rust/issues/2713) */
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    @ExpandMacros(MacroExpansionScope.ALL, "std")
    fun `test issue 2713`() = checkByText("""
        fn main() { u64::from(0u8); }
    """)

    fun `test assoc fn of type param with multiple bounds of the same trait`() = checkByText("""
        trait From<T> { fn from(_: T) -> Self; }
        fn foo<T: From<u8> + From<i8>>(_: T) {
            T::from(0u8);
            T::from(0i8);
        }
    """)

    /** Issue [3125](https://github.com/intellij-rust/intellij-rust/issues/3125) */
    @ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
    fun `test issue 3125`() = checkByText("""
        struct S;

        trait Trait {
            type Item;
            fn foo() -> Self::Item;
        }

        impl Trait for S {
            type Item = &'static str;
            fn foo() -> Self::Item { <error>0</error> }
        }
    """)

    fun `test type mismatch E0308 yield expr`() = checkByText("""
        fn main() {
            || {
                yield 0;
                yield <error>"string"</error>;
            };
        }
    """)
}
