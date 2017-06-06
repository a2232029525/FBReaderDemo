# FBReaderDemo
androidstudio版FBReader，可直接导入项目中进行依赖
FBReader是一款功能强大的开源阅读器，本人主要用来进行epub的解析与展示



1.项目的application一定要继承FBReaderApplication；
2.FBReaderIntents类中的DEFAULT_PACKAGE一定要改成自己项目的包名；
3.FBReader设置里面的初始参数在geometerplus-->fbreader-->fbreader-->options的类中进行更改；
4.在遇到打开FBReader显示空白页时，多半是因为和你项目中的第三方不兼容，在build.gradle的android节点下添加
    splits {
        abi {
            enable true
            reset()
            include 'armeabi-v7a','x86'
        }
    }
    可以解决冲突的问题
