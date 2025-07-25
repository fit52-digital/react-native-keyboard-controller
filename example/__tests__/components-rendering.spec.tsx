import { render } from "@testing-library/react-native";
import React from "react";
import { View } from "react-native";
import {
  KeyboardAvoidingView,
  KeyboardAwareScrollView,
  KeyboardBackgroundView,
  KeyboardControllerView,
  KeyboardExtender,
  KeyboardProvider,
  KeyboardStickyView,
  KeyboardToolbar,
  OverKeyboardView,
} from "react-native-keyboard-controller";

function EmptyView() {
  return <View style={{ width: 20, height: 20, backgroundColor: "black" }} />;
}

function KeyboardControllerViewTest() {
  return <KeyboardControllerView statusBarTranslucent />;
}

function KeyboardProviderTest() {
  return (
    <KeyboardProvider statusBarTranslucent>
      <EmptyView />
    </KeyboardProvider>
  );
}

const style = { marginBottom: 20 };

function KeyboardAvoidingViewTest() {
  return (
    <KeyboardAvoidingView behavior="height" style={style}>
      <EmptyView />
    </KeyboardAvoidingView>
  );
}

function KeyboardAwareScrollViewTest() {
  return (
    <KeyboardAwareScrollView
      bottomOffset={20}
      disableScrollOnKeyboardHide={false}
      enabled={true}
      style={style}
    >
      <EmptyView />
    </KeyboardAwareScrollView>
  );
}

const offset = { closed: -20, opened: -40 };

function KeyboardStickyViewTest() {
  return (
    <KeyboardStickyView offset={offset} style={style}>
      <EmptyView />
    </KeyboardStickyView>
  );
}

const content = <EmptyView />;

function KeyboardToolbarTest() {
  return <KeyboardToolbar content={content} />;
}

function OverKeyboardViewTest() {
  return (
    <OverKeyboardView visible={true}>
      <EmptyView />
    </OverKeyboardView>
  );
}

function KeyboardBackgroundViewTest() {
  return <KeyboardBackgroundView />;
}

function KeyboardExtenderTest() {
  return <KeyboardExtender enabled={true}>{<EmptyView />}</KeyboardExtender>;
}

describe("components rendering", () => {
  it("should render `KeyboardControllerView`", () => {
    expect(render(<KeyboardControllerViewTest />)).toMatchSnapshot();
  });

  it("should render `KeyboardProvider`", () => {
    expect(render(<KeyboardProviderTest />)).toMatchSnapshot();
  });

  it("should render `KeyboardAvoidingView`", () => {
    expect(render(<KeyboardAvoidingViewTest />)).toMatchSnapshot();
  });

  it("should render `KeyboardAwareScrollView`", () => {
    expect(render(<KeyboardAwareScrollViewTest />)).toMatchSnapshot();
  });

  it("should render `KeyboardStickyView`", () => {
    expect(render(<KeyboardStickyViewTest />)).toMatchSnapshot();
  });

  it("should render `KeyboardToolbar`", () => {
    expect(render(<KeyboardToolbarTest />)).toMatchSnapshot();
  });

  it("should render `OverKeyboardView`", () => {
    expect(render(<OverKeyboardViewTest />)).toMatchSnapshot();
  });

  it("should render `KeyboardBackgroundView`", () => {
    expect(render(<KeyboardBackgroundViewTest />)).toMatchSnapshot();
  });

  it("should render `KeyboardExtenderTest`", () => {
    expect(render(<KeyboardExtenderTest />)).toMatchSnapshot();
  });
});
