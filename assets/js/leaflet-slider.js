L.Control.Slider = L.Control.extend({
    update: function(value){
        return value;
    },

    options: {
        size: '100px',
        position: 'topright',
        min: 0,
        max: 250,
        step: 1,
        id: "slider",
        value: 50,
        collapsed: true,
        title: 'Leaflet Slider',
        logo: 'S',
        orientation: 'horizontal',
        getValue: function(value) {
            return value;
        },
        showValue: true
    },
    initialize: function (f, options) {
        L.setOptions(this, options);
        if (typeof f == "function") {
            this.update = f;
        } else {
            this.update = function (value) {
                console.log(value);
            };
        }
        if (typeof this.options.getValue != "function") {
            this.options.getValue = function (value) {
                return value;
            };
        }
        if (this.options.orientation!='vertical') {
            this.options.orientation = 'horizontal';
        }
    },
    onAdd: function (map) {
        this._initLayout();
        this.update(this.options.value+"");
        return this._container;
    },
    _updateValue: function () {
        if (this.options.showValue){
    	   this._sliderValue.innerHTML = this.options.getValue(this.value = this.slider.value) + 'km';
        }
        this.update(this.value);
    },
    _initLayout: function () {
        var className = 'leaflet-control-slider';
        this._container = L.DomUtil.create('div', className + ' ' +className + '-' + this.options.orientation);
        this._sliderLink = L.DomUtil.create('a', className + '-toggle', this._container);
        this._sliderLink.setAttribute("title", this.options.title);
        this._sliderLink.innerHTML = this.options.logo;
        if (this.options.showValue){
            this._sliderValue = L.DomUtil.create('p', className+'-value', this._container);
            this._sliderValue.innerHTML = this.options.getValue(this.options.value) + 'km';
        }
        this.slider = L.DomUtil.create('input', 'leaflet-slider', this._container);
        if (this.options.orientation == 'vertical') {this.slider.setAttribute("orient", "vertical");}
        this.slider.setAttribute("title", this.options.title);
        this.slider.setAttribute("id", this.options.id);
        this.slider.setAttribute("type", "range");
        this.slider.setAttribute("min", this.options.min);
        this.slider.setAttribute("max", this.options.max);
        this.slider.setAttribute("step", this.options.step);
        this.slider.setAttribute("value", this.options.value);
        this.slider.setAttribute("onChange", this.options.id + "._updateValue()");

        if (this.options.showValue){
            if (this.options.orientation =='vertical') {this.slider.style.height = (this.options.size.replace('px','') -36) +'px';}
            else {this.slider.style.width = (this.options.size.replace('px','') -56) +'px';}
        } else {
            if (this.options.orientation =='vertical') {this.slider.style.height = (this.options.size.replace('px','') -10) +'px';}
            else {this.slider.style.width = (this.options.size.replace('px','') -25) +'px';}
        }
20
        L.DomEvent.disableClickPropagation(this._container);

        if (this.options.collapsed) {
            if (!L.Browser.android) {
                L.DomEvent.on(this._container, {
                    mouseenter: this._expand,
                    mouseleave: this._collapse
                }, this);
            }

            if (L.Browser.touch) {
                L.DomEvent
                    .on(this._sliderLink, 'click', L.DomEvent.stop)
                    .on(this,_sliderLink, 'click', this._expand, this);
            } else {
                L.DomEvent.on(this._sliderLink, 'focus', this._expand, this);
            }
        } else {
            this._expand();
        }
    },
    _expand: function () {
        L.DomUtil.addClass(this._container, 'leaflet-control-slider-expanded');
    },
    _collapse: function () {
        L.DomUtil.removeClass(this._container, 'leaflet-control-slider-expanded');
    }


});
L.control.slider = function (f, options) {
    return new L.Control.Slider(f, options);
 };