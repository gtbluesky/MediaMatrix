precision highp float;
uniform sampler2D uTextureUnit;
varying highp vec2 vTextureCoord;
uniform float uViewWidth;
uniform float uViewHeight;
uniform float uMosaicSize;

void main() {
    //将纹理坐标转换为View中对应的像素值
    vec2 coordInView = vec2(vTextureCoord.x * uViewWidth, vTextureCoord.y * uViewHeight);
    //获取每个马赛克区域中心的像素值
    vec2 centerInView = vec2(
        uMosaicSize * (floor(coordInView.x / uMosaicSize) + 0.5),
        uMosaicSize * (floor(coordInView.y / uMosaicSize) + 0.5)
    );
    if (length(centerInView - coordInView) < 0.5 * uMosaicSize) {
        gl_FragColor = texture2D(
            uTextureUnit,
            vec2(centerInView.x / uViewWidth, centerInView.y / uViewHeight)
        );
    } else {
        gl_FragColor = texture2D(uTextureUnit, vTextureCoord);
    }
}