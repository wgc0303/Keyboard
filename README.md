# CustomKeyboard

Android 自定义键盘组件库，提供 `CustomKeyboardEditText`，用于在业务页面中替代系统软键盘输入。当前工程同时包含：

- `keyboard`：可复用的组件库模块
- `app`：接入示例和宿主验证 Demo

支持的键盘类型：

- 数字键盘
- 身份证键盘
- 数字密码键盘
- 字母数字切换键盘

## 模块结构

```text
CustomKeyboard/
├─ app/        # Demo 与宿主验证
└─ keyboard/   # 组件库
```

## 引入方式

工程内直接依赖模块：

```kotlin
dependencies {
    implementation(project(":keyboard"))
}
```

如果后续通过 Maven 使用，当前库配置的坐标是：

```kotlin
implementation("io.github.wgc0303:keyboard:0.0.2")
```

## 快速开始

在布局中声明 `CustomKeyboardEditText`：

```xml
<cn.wgc.keyboard.CustomKeyboardEditText
    android:id="@+id/passwordInput"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:hint="请输入密码"
    app:ck_keyboardType="numberPassword" />
```

支持的键盘类型：

```xml
app:ck_keyboardType="number"
app:ck_keyboardType="idCard"
app:ck_keyboardType="numberPassword"
app:ck_keyboardType="alphaNumber"
```

也可以在代码中切换：

```kotlin
editText.keyboardType = CustomKeyboardType.NUMBER_PASSWORD
```

## 必要接入项

普通 `Activity` / `Fragment` 场景，最少只需要两步：

1. 使用 `CustomKeyboardEditText`
2. 在宿主 Activity 安装外部点击收起逻辑

```kotlin
CustomKeyboardManager.installDismissOnOutsideTouch(this)
```

这个调用会带来几件事：

- 普通 `EditText` 仍然走系统键盘
- 点击普通 `EditText` 外部会收起系统键盘
- 点击 `CustomKeyboardEditText` 会切到自定义键盘
- 自定义键盘显示时，返回键优先收起自定义键盘

如果需要自定义行为：

```kotlin
CustomKeyboardManager.installDismissOnOutsideTouch(
    activity = this,
    options = KeyboardDismissOptions(
        dismissSystemKeyboard = true,
        clearSystemEditTextFocus = true,
    ),
)
```

## `EdgeToEdgeHelper` 是不是必须

不是。

`app` 模块里的 `EdgeToEdgeHelper` 只是 Demo 为了统一验证 `Activity`、`Fragment`、`DialogFragment`、自定义 `Dialog`、`BottomSheet` 的状态栏 / 导航栏 / IME inset 行为提供的一层示例封装，不是组件库接入的硬依赖。

接入时可以这样理解：

- 页面没有做沉浸式：不需要使用 `EdgeToEdgeHelper`
- 页面已经有自己的 edge-to-edge / insets 方案：继续沿用你自己的方案即可
- `Dialog`、自定义 `Dialog`、`BottomSheet` 这类特殊宿主：需要参考 Demo 的 `Window` 绑定方式

当前不把 `PopupWindow` 作为正式支持宿主，未纳入组件库稳定适配范围。

## `Dialog` / `BottomSheet` 宿主接入

这类宿主建议在内容根节点上绑定宿主 `Window`：

```kotlin
CustomKeyboardManager.bindHostWindow(
    root = contentRoot,
    window = dialog.window,
)
```

适用场景：

- `DialogFragment`
- 自定义 `Dialog`
- `BottomSheetDialogFragment`

这样做的目的，是让键盘管理器在非 Activity 宿主里也能拿到正确的窗口上下文，处理返回键、导航栏区域和布局层级。

## 常用配置

### 字母数字键盘初始页

`alphaNumber` 默认先显示数字页。如需先显示字母页：

```xml
app:ck_alphaInitialMode="letter"
```

```kotlin
editText.alphaInitialMode = AlphaKeyboardInitialMode.LETTER
```

### 禁用空格和点号

仅对 `alphaNumber` 生效：

```xml
app:ck_disableSpace="true"
app:ck_disableDot="true"
```

### 随机数字键

适用于数字密码场景：

```xml
app:ck_randomNumberKeys="true"
```

```kotlin
editText.randomNumberKeys = true
```

### 按键间距

默认 `0dp`，表现更接近系统平铺数字键盘：

```xml
app:ck_keyGap="0dp"
```

设置为大于 `0dp` 后，会变成带卡片间距的布局：

```xml
app:ck_keyGap="6dp"
```

## 输入限制

`CustomKeyboardEditText` 内置输入过滤，能防止粘贴内容绕过当前键盘规则：

- `number`：只允许 `0-9`
- `numberPassword`：只允许 `0-9`
- `idCard`：只允许 `0-9` 和 `X/x`，其中 `x` 会转成 `X`
- `alphaNumber`：允许字母、数字，空格和点号是否允许取决于配置

## 样式配置

可以通过 XML 调整键盘颜色、按键背景、分割线、图标和按键高度。

```xml
<cn.wgc.keyboard.CustomKeyboardEditText
    android:layout_width="match_parent"
    android:layout_height="48dp"
    app:ck_keyboardType="number"
    app:ck_keyboardBackgroundColor="#FFF7ED"
    app:ck_keyTextColor="#7C2D12"
    app:ck_dividerColor="#F59E0B"
    app:ck_keyHeight="54dp"
    app:ck_keyGap="0dp" />
```

常用样式属性：

- `ck_keyboardBackgroundColor`：平铺键盘底色
- `ck_spacedKeyboardBackgroundColor`：带间距模式的键盘底色
- `ck_letterRowBackgroundColor`：字母键盘未铺满区域的底色
- `ck_systemNavFillColor`：手势导航底部填充色，以及传统导航栏场景下用于同步的底部颜色
- `ck_keyTextColor`：按键文字颜色
- `ck_dividerColor`：分割线颜色
- `ck_keyHeight`：按键高度
- `ck_panelPaddingWhenSpaced`：带间距模式下的面板内边距
- `ck_keyBackground`：普通按键背景
- `ck_functionKeyBackground`：功能键背景
- `ck_flatKeyBackground`：平铺普通按键背景
- `ck_flatEdgeKeyBackground`：平铺边缘功能键背景

图标属性：

- `ck_deleteIcon`
- `ck_visibleIcon`
- `ck_invisibleIcon`
- `ck_shiftIcon`
- `ck_shiftActiveIcon`
- `ck_hideKeyboardIcon`

## 代码方式自定义样式

```kotlin
editText.keyboardStyle = CustomKeyboardStyle.default(editText.context).copy(
    keyTextColor = Color.BLACK,
    deleteIconRes = R.drawable.ic_custom_delete,
)
```

同一个宿主页面里多个 `CustomKeyboardEditText` 共用一套键盘实例，但切换焦点时会应用当前输入框自己的配置。

## 宿主验证 Demo

`app` 模块现在拆成了独立验证入口，便于分别回归不同宿主：

- `Activity 宿主验证`
- `Fragment 宿主验证`
- `Fragment 混合输入验证`
- `DialogFragment 宿主验证`
- `自定义 Dialog 宿主验证`
- `BottomSheet 宿主验证`

入口页面见：

- `app/src/main/java/cn/wgc/keyboard/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`

## 说明

- `PwdEditText` 当前位于 `app` 模块中，属于 Demo 验证控件，不属于 `keyboard` 库对外 API
- Demo 中的 `EdgeToEdgeHelper`、场景 Activity、Dialog/BottomSheet 验证页主要用于验证宿主兼容性，不要求业务方原样照搬
