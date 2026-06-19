# Custom Keyboard 接入说明

`keyboard` 模块提供 `CustomKeyboardEditText`，用于在业务页面中直接唤起自定义键盘。支持数字、身份证、数字密码、字母数字切换键盘，并支持样式、图标、按键高度、随机数字键等配置。

## 1. 引入模块

如果工程内直接依赖本地模块：

```kotlin
dependencies {
    implementation(project(":keyboard"))
}
```

## 2. 基础使用

在 XML 中使用 `cn.wgc.keyboard.CustomKeyboardEditText`：

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

也可以在代码中配置：

```kotlin
editText.keyboardType = CustomKeyboardType.NUMBER_PASSWORD
```

## 3. 常用功能配置

### 字母数字键盘初始模式

`alphaNumber` 类型默认先显示数字键盘。如需先显示字母键盘：

```xml
app:ck_alphaInitialMode="letter"
```

代码：

```kotlin
editText.alphaInitialMode = AlphaKeyboardInitialMode.LETTER
```

### 禁用空格和点号

仅对 `alphaNumber` 生效。禁用后按键仍有点击反馈，但不会输入字符，并且粘贴内容也会被过滤。

```xml
app:ck_disableSpace="true"
app:ck_disableDot="true"
```

### 随机数字键

可用于数字密码键盘。开启后每次显示键盘会随机排列 `0-9`。

```xml
app:ck_randomNumberKeys="true"
```

代码：

```kotlin
editText.randomNumberKeys = true
```

### 按键间距

默认 `0dp`，即系统键盘风格的平铺分割线样式。

```xml
app:ck_keyGap="0dp"
```

设置大于 `0dp` 后会切换为有间距的卡片式按键：

```xml
app:ck_keyGap="6dp"
```

## 4. 输入限制

`CustomKeyboardEditText` 内置输入过滤，防止粘贴绕过键盘规则：

- `number`：只允许 `0-9`
- `numberPassword`：只允许 `0-9`
- `idCard`：允许 `0-9` 和 `X/x`，小写 `x` 会转为 `X`
- `alphaNumber`：允许字母、数字，是否允许空格和点号取决于配置

不会禁用粘贴，只会过滤不符合当前类型的字符。

## 5. 样式配置

可以在 XML 中配置键盘底色、分割线、文字颜色、按键高度、按键背景和图标。

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
- `ck_spacedKeyboardBackgroundColor`：有间距键盘底色
- `ck_letterRowBackgroundColor`：字母键盘未铺满区域底色
- `ck_keyTextColor`：按键文字颜色
- `ck_dividerColor`：分割线颜色
- `ck_keyHeight`：按键高度
- `ck_panelPaddingWhenSpaced`：有间距模式下键盘内边距
- `ck_keyBackground`：普通按键背景
- `ck_functionKeyBackground`：功能键背景
- `ck_flatKeyBackground`：平铺普通按键背景
- `ck_flatEdgeKeyBackground`：平铺边缘功能键背景

## 6. 图标配置

接入方可以替换删除、显示/隐藏密码、Shift 和收起键盘图标。

```xml
<cn.wgc.keyboard.CustomKeyboardEditText
    android:layout_width="match_parent"
    android:layout_height="48dp"
    app:ck_keyboardType="alphaNumber"
    app:ck_alphaInitialMode="letter"
    app:ck_deleteIcon="@drawable/ic_custom_delete"
    app:ck_shiftIcon="@drawable/ic_custom_shift"
    app:ck_shiftActiveIcon="@drawable/ic_custom_shift_active" />
```

图标属性：

- `ck_deleteIcon`
- `ck_visibleIcon`
- `ck_invisibleIcon`
- `ck_shiftIcon`
- `ck_shiftActiveIcon`
- `ck_hideKeyboardIcon`

## 7. 代码配置样式

如果 XML 不够灵活，可以在代码中设置 `CustomKeyboardStyle`：

```kotlin
editText.keyboardStyle = CustomKeyboardStyle.default(editText.context).copy(
    keyHeight = 56.dp,
    keyTextColor = Color.BLACK,
    deleteIconRes = R.drawable.ic_custom_delete
)
```

如果同一个 Activity 中有多个 `CustomKeyboardEditText`，键盘实例会共用，但每次切换焦点都会应用当前输入框自己的配置。

## 8. 点击外部收起系统键盘

默认只处理自定义键盘。普通 `EditText` 的系统键盘外部点击收起，需要接入方手动安装：

```kotlin
CustomKeyboardManager.installDismissOnOutsideTouch(this)
```

可配置：

```kotlin
CustomKeyboardManager.installDismissOnOutsideTouch(
    activity = this,
    options = KeyboardDismissOptions(
        dismissSystemKeyboard = true,
        clearSystemEditTextFocus = true
    )
)
```

安装后：

- 普通 `EditText` 获得焦点时仍显示系统键盘
- 点击普通 `EditText` 外部会收起系统键盘
- 点击系统导航栏区域不会触发收起
- 点击 `CustomKeyboardEditText` 会切换到自定义键盘

## 9. 返回键行为

自定义键盘显示时，系统返回键会优先收起自定义键盘；自定义键盘未显示时，返回键继续执行 Activity 原有返回行为。

## 10. 导航栏适配

键盘会显示在系统导航栏上方。手势导航下底部小横条区域会用键盘背景色填充，避免露出页面底色造成割裂；传统三键导航栏保持系统区域原样。

## 11. 示例

完整验证示例见 app 模块：

- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/java/cn/wgc/keyboard/MainActivity.kt`
