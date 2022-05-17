# 简介
**plugin**
用于android gradle项目插件，让其支持smali和java混合开发

# 使用
[![](https://jitpack.io/v/Mosect/Android-SmaliPlugin.svg)](https://jitpack.io/#Mosect/Android-SmaliPlugin)

## 1. 添加jitpack
在根项目build.gradle文件中，添加仓库：
```
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

## 2. 添加classpath
在根项目build.gradle文件中，添加classpath：
```
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        // 自己的com.android.tools.build:gradle插件，支持3.4.0+
        // classpath "com.android.tools.build:gradle:3.4.0"
        // Android-SmaliPlugin
        classpath 'com.github.Mosect:Android-SmaliPlugin:1.1.1-b11'
    }
}
```

## 3. 启用插件
在Android app模块的build.gradle中，启用插件：
```
// Android插件
//apply plugin: 'com.android.application'
// 在Android插件之后，启用Android-SmaliPlugin
apply plugin: 'com.mosect.smali.plugin.SmaliPlugin'
```

## 4. 添加smali代码
在Android app模块目录下，创建src/main/smali/classes文件夹，讲smali文件放进此文件夹即可构建java+smali的apk和aab

### a. 多口味smali源码文件夹配置
默认情况下，在sourceSet目录创建smali/classes文件夹，讲smali放进去即可，如果配置了productFlavors，可以在其sourceSet目录创建smali/classes即可，其源码位置类似java。

比如：
* 默认sourceSet smali源码位置：src/main/smali/classes
* debug sourceSet smali源码位置：src/debug/smali/classes
* release sourceSet smali源码位置：src/release/smali/classes
* beta（productFlavors配置） sourceSet smali源码位置：src/beta/smali/classes


### b. 多dex配置
smali/classes的源码编译后存放到classes.dex，如需存放到其他dex，可以将smali源码放在smali/${dex_name}，有效的dex_name：classes、classes2、classes3 ... classes99

### c. 自定义smali源码目录
在Android app模块build.gradle文件中：

```
android {
    sourceSets {
        main {
            smali {
                // 完全使用自己定义的目录
                setDirs([file('my-smali/a'), file('my-smali/b')])
                // 添加自己定义的目录
                addDir(file('my-smali/a'))
                // 添加多个目录
                addDirs(file('my-smali/a'), file('my-smali/b'))
            }
        }
    }
}
```

其他sourceSet同理

### d. 指定smali编译apiLevel
默认smali apiLevel为15，无特殊需求，一般不用设置此选项
```
android {
    sourceSets {
        main {
            smali {
                apiLevel = 16
            }
        }
    }
}
```

### e. 额外规则：移动类到指定dex：class-position.txt
如果需要批量或者移动很多类，又不方便调整smali源码目录情况下，可以使用class-position.txt文件来定义类存放位置规则：

有两种方式：
1. 在Android app模块目录下创建class-position.txt文件，或者在某个sourceSet目录下创建，例如src/main/class-position.txt，多个sourceSet都配置，其规则将进行叠加
2. 在Android app模块build.gradle文件中：
```
android {
    sourceSets {
        main {
            smali {
                // 完全使用自己定义的文件
                setPositionFiles([file('my/class-position.txt')])
                // 添加单个文件
                addPositionFile(file('my/class-position.txt'))
                // 添加多个文件
                addPositionFiles(file('my/class-position.txt'), file('my2/class-position.txt'))
            }
        }
    }
}
```

文件示例及规则：
```
# #开头为注释
# 移动类到最后一个dex
move	0:com.mosect.Test		end
# 移动类到新的dex
move	1:com.mosect.*			new
# 移动类到第二个dex，即classes2.dex
move	2:com.mosect.**			2
# 移动类到主dex，即classes.dex
move  0:com.mosect.Test   main
# 删除类
delete  2:com.mosect.MyTest
```
* 指令规则：move|delete class_path target，move表示移动类，delete表示删除类，delete没有target参数
* 类路径格式：${dex_index}:${class_path}，有效的dex_index：0-99；0表示不指定dex，即所有dex中匹配到就指定相应动作。class_path可以包含 \*或者\*\* 来匹配多个类，使用java格式类路径

## java+smali编程
在Android app模块build.gradle中，引用sdk：
```
dependencies {
    implementation 'com.github.Mosect:Android-SmaliSdk:1.1.1-b1'
}
```
在包com.mosect.smali.annotation下提供5种注解，此注解不会被打包进dex，用于Android-SmaliPlugin处理，以下为注解说明（按处理优先级）：

### @Delete 注解
删除smali和java源码中的类、方法或字段，

### @Original 注解
使用smali源码中的类、方法或字段，即忽略java中的类、方法或字段

### @Replace 注解
替换smali源码中的类、方法或字段，即只使用java中的类、方法或字段

### @Merge 注解
合并java和smali类

### @Ignore 注解
忽略java中类、方法或字段，即java中的类、方法或字段不会打包进dex

java调用smali源码，需要在java中创建对应的类，然后使用@Ignore注解即可。这5个注解可以相互配合，组成所需的开发需求

## 使用class-operation.txt更改java与smali合并规则
除了使用注解之外，还可以使用class-operation.txt文件规定合并规则，其配置和class-position.txt类似。

有两种方式：
1. 在Android app模块目录下创建class-operation.txt文件，或者在某个sourceSet目录下创建，例如src/main/class-operation.txt，多个sourceSet都配置，其规则将进行叠加
2. 在Android app模块build.gradle文件中：
```
android {
    sourceSets {
        main {
            smali {
                // 完全使用自己定义的文件
                setOperationFiles([file('my/class-operation.txt')])
                // 添加单个文件
                addOperationFile(file('my/class-operation.txt'))
                // 添加多个文件
                addOperationFiles(file('my/class-operation.txt'), file('my2/class-operation.txt'))
            }
        }
    }
}
```
文件示例及规则：
```
# #开头为注释
ignore		com.mosect.Test
delete		com.mosect.*
original	com.mosect.**
replace   com.mosect.*
merge		com.mosect.Test
	ignore		field1
	delete		*
	replace		field4
	ignore		method1()
	delete		method2(String,int)
	original	method3(java.land.Date,Integer)
```
* 指令规则：ignore|delete|original|merge|replace class_path

    其指令和注解一致
    
    字段、方法合并，需要在merge之后，并且以空白字符开头，推荐使用\t
    
    字段只需写明字段名，支持\*和\*\*；方法需要写明方法名和参数签名，例如：
    
      met(Ljava.land.String)
      
* 类路径格式：class_path。class_path可以包含 \*或者\*\* 来匹配多个类，使用java格式类路径

# 版本更新记录
## 1.1.1-b10
```
1. 兼容 AGP 3.3.0-7.2
2. 兼容MultiDex和R8
```
## 1.1.1-b1
```
beta测试版本
1. 实现基本smali和java混合开发
```

# 意见反馈
* 提issue
* 加QQ群聊：624420724
* 个人主页：http://mosect.com
