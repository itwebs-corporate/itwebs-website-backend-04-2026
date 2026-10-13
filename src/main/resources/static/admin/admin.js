'use strict';
const $ = id => document.getElementById(id);
const sections = {content:'Тексты сайта',media:'Картинки',leads:'Заявки',articles:'Статьи',categories:'Категории'};
let auth='', section='content', rows=[], media=[], categories=[], page=0, totalPages=0, current=null, busy=false, generation=0;
const path = kind => '/api/admin/' + (['articles','categories'].includes(kind)?'blog/':'') + kind;
function el(tag,text,cls){const n=document.createElement(tag);if(text!==undefined)n.textContent=text;if(cls)n.className=cls;return n;}
function button(text,action){const b=el('button',text);b.type='button';b.onclick=action;return b;}
function notify(message,success=false){$('notice').textContent=message;$('notice').className=success?'success':'';}
async function api(url,method='GET',data){const headers={Authorization:auth};if(data && !(data instanceof FormData))headers['Content-Type']='application/json';const r=await fetch(url,{method,headers,credentials:'omit',body:data?(data instanceof FormData?data:JSON.stringify(data)):undefined});if(!r.ok){let p={};try{p=await r.json();}catch{}if(r.status===401)throw Error('Неверный логин или пароль. Войдите заново.');throw Error(p.detail||p.message||`Ошибка ${r.status}. Попробуйте обновить данные.`);}return r.status===204?null:r.json();}
$('login').onsubmit=async e=>{e.preventDefault();const f=e.target;auth='Basic '+btoa(String.fromCharCode(...new TextEncoder().encode(f.username.value+':'+f.password.value)));const b=f.querySelector('button');b.disabled=true;try{await api(path('categories'));f.reset();f.hidden=true;$('app').hidden=false;$('logout').hidden=false;await load();}catch(e){auth='';notify(e.message);}finally{b.disabled=false;}};
$('logout').onclick=()=>{generation++;auth='';rows=[];media=[];categories=[];current=null;$('list').replaceChildren();$('fields').replaceChildren();$('editor').close();$('app').hidden=true;$('logout').hidden=true;$('login').hidden=false;notify('');};
for(const [key,title] of Object.entries(sections))$('nav').append(button(title,async()=>{section=key;page=0;await load();}));
$('refresh').onclick=()=>load();$('create').onclick=()=>openEditor();$('close').onclick=()=>{if(!busy)$('editor').close();};
$('editor').addEventListener('cancel',e=>{if(busy)e.preventDefault();});
async function load(){const ticket=++generation,kind=section;notify('');$('list').textContent='Загрузка…';$('title').textContent=sections[kind];$('create').hidden=kind==='leads';[...$('nav').children].forEach((b,i)=>b.classList.toggle('active',Object.keys(sections)[i]===kind));try{const data=await api(path(kind)+(kind==='articles'?`?page=${page}&size=20`:''));if(ticket!==generation)return;rows=kind==='articles'?data.items:data;totalPages=data.totalPages||0;filters();render();}catch(e){if(ticket===generation){$('list').textContent='Не удалось загрузить данные.';notify(e.message);}}}
function filters(){const box=$('filters');box.replaceChildren();const search=el('input');search.placeholder='Поиск';search.id='search';search.oninput=render;box.append(search);if(section==='content'){const select=el('select');select.id='sectionFilter';select.append(new Option('Все разделы',''));[...new Set(rows.map(r=>r.sectionName))].sort().forEach(s=>select.append(new Option(s,s)));select.onchange=render;box.append(select);}}
function render(){const root=$('list');root.replaceChildren();const q=($('search')?.value||'').toLowerCase(),filter=$('sectionFilter')?.value;const visible=rows.filter(r=>(!filter||r.sectionName===filter)&&JSON.stringify(r).toLowerCase().includes(q));if(!visible.length)root.append(el('p','Пока ничего нет.','empty'));for(const r of visible){const card=el('div',undefined,'card');if(section==='media'){const img=el('img');img.src=r.url;img.alt=r.altText||'';card.append(img);}const body=el('div',undefined,'body');let title,description;if(section==='content'){title=r.label||r.itemKey;description=`${r.sectionName} / ${r.groupName||'—'} / ${r.itemKey}\n${r.value||r.href||''}\n${r.published?'Опубликовано':'Скрыто'}`;}else if(section==='media'){title=r.originalName;description=`${r.altText||'Без описания'} · ${Math.round(r.sizeBytes/1024)} КБ`;}else if(section==='leads'){title=r.name;description=`${r.processed?'Обработана':'Новая'} · ${new Date(r.createdAt).toLocaleString('ru-RU')}\n${r.phone||''} ${r.email||''}\n${r.message||''}`;}else if(section==='articles'){title=r.title;description=`${r.status==='DRAFT'?'Черновик':'Публикация'} · ${r.slug}`;}else{title=r.name;description=r.slug;}body.append(el('strong',title),el('p',description));card.append(body,button(section==='leads'?'Открыть':'Редактировать',()=>openEditor(r.id)));root.append(card);}if(section==='articles'){const p=el('div',undefined,'pager'),prev=button('← Назад',()=>{page--;load();}),next=button('Далее →',()=>{page++;load();});prev.disabled=page===0;next.disabled=page+1>=totalPages;p.append(prev,el('span',`Страница ${page+1} из ${Math.max(1,totalPages)}`),next);root.append(p);}}
function field(name,title,value='',type='text',options){const label=el('label',title);let input;if(options){input=el('select');for(const [v,t] of options)input.append(new Option(t,v));input.value=value??'';}else{input=el(type==='textarea'?'textarea':'input');if(type!=='textarea')input.type=type;if(type==='checkbox')input.checked=!!value;else input.value=value??'';}input.name=name;label.append(input);$('fields').append(label);return input;}
function imageField(name,title,value){
    const options=[['','Не выбрано'],...media.map(m=>[String(m.id),m.originalName+' — '+m.altText])];
    if(value&&!media.some(m=>m.id===value))options.push([String(value),'Выбранная картинка']);
    const select=field(name,title,value||'','select',options);
    attachImageUpload(select,()=>{if(name==='mediaId'&&current?.kind==='content')$('fields').querySelector('[name=type]').value='IMAGE';});
    return select;
}
function attachImageUpload(select,onUploaded=()=>{}){
    select.classList.add('image-select');
    const tools=el('div',undefined,'image-tools'),file=el('input'),preview=el('img'),status=el('p');
    file.type='file';file.accept='image/jpeg,image/png,image/webp,image/gif,image/avif';file.hidden=true;
    preview.className='image-preview';preview.hidden=true;status.className='image-status';status.setAttribute('role','status');
    function showPreview(){const selected=media.find(m=>String(m.id)===select.value);preview.hidden=!selected;if(selected){preview.src=selected.url;preview.alt=selected.altText||selected.originalName;}else preview.removeAttribute('src');}
    select.addEventListener('change',showPreview);
    const upload=button('Загрузить картинку',()=>{if(!busy)file.click();});upload.className='image-upload';
    tools.append(upload,file,el('small','JPEG, PNG, WebP, GIF, AVIF — до 15 МБ'),preview,status);
    select.parentElement.after(tools);showPreview();
    file.onchange=async()=>{
        const chosen=file.files[0];file.value='';if(!chosen||busy)return;
        status.textContent='';status.classList.remove('error');
        if(chosen.size>15*1024*1024){status.textContent='Файл больше 15 МБ. Выберите меньшую картинку.';status.classList.add('error');return;}
        if(!['image/jpeg','image/png','image/webp','image/gif','image/avif'].includes(chosen.type)){status.textContent='Выберите JPEG, PNG, WebP, GIF или AVIF.';status.classList.add('error');return;}
        const editing=current;lock(true);status.textContent='Загрузка…';
        try{
            const body=new FormData();body.append('file',chosen);body.append('altText',chosen.name.slice(0,255)||'Картинка');
            const uploaded=await api(path('media'),'POST',body);
            if(current!==editing||!select.isConnected||!auth)return;
            media.push(uploaded);
            for(const other of $('fields').querySelectorAll('.image-select'))other.append(new Option(uploaded.originalName+' — '+uploaded.altText,String(uploaded.id)));
            select.value=String(uploaded.id);select.dispatchEvent(new Event('change'));onUploaded();
            status.textContent='Картинка загружена и выбрана. Нажмите «Сохранить», чтобы применить её.';
        }catch(e){if(select.isConnected){status.textContent=e.message;status.classList.add('error');}}
        finally{lock(false);}
    };
}
function required(input,max){input.required=true;if(max)input.maxLength=max;return input;}
async function openEditor(id){const kind=section,ticket=generation;notify('');try{const data=id?await api(path(kind)+'/'+id):{};if(kind!==section||ticket!==generation||!auth)return;current={kind,id,data};$('fields').replaceChildren();$('editError').textContent='';$('editTitle').textContent=(id?'Редактировать: ':'Добавить: ')+sections[kind];$('delete').hidden=!id;$('save').textContent=kind==='media'&&!id?'Загрузить':'Сохранить';
if(kind==='content'){required(field('sectionName','Раздел',data.sectionName),80);required(field('itemKey','Ключ поля',data.itemKey),80);field('groupName','Группа',data.groupName);field('type','Тип',data.type||'TEXT','select',[['TEXT','Текст'],['LINK','Ссылка'],['IMAGE','Картинка']]);field('label','Название в админке',data.label);field('value','Текст',data.value,'textarea');field('href','Адрес ссылки',data.href);imageField('mediaId','Картинка',data.mediaId);field('sortOrder','Порядок',data.sortOrder||0,'number');field('published','Показывать на сайте',data.published??true,'checkbox');}
if(kind==='media'){if(!id){const f=required(field('file','Файл (до 15 МБ)','','file'));f.accept='image/jpeg,image/png,image/webp,image/gif,image/avif';}required(field('altText','Описание картинки',data.altText),255);if(id){const img=el('img');img.src=data.url;img.alt=data.altText;img.style.maxWidth='100%';$('fields').append(img);}}
if(kind==='categories'){required(field('name','Название',data.name),80);const slug=required(field('slug','Адрес: латиница, цифры и дефисы',data.slug),80);slug.pattern='[a-z0-9]+(?:-[a-z0-9]+)*';field('sortOrder','Порядок',data.sortOrder||0,'number').min=0;}
if(kind==='leads'){for(const [key,title] of Object.entries({name:'Имя',phone:'Телефон',email:'Почта',message:'Сообщение',contactMethod:'Как связаться',createdAt:'Дата',amoCrmSyncStatus:'Передача в amoCRM',amoCrmLeadId:'Номер сделки',amoCrmSyncError:'Ошибка amoCRM'})){const line=el('p');line.append(el('strong',title+': '),el('span',data[key]??'—'));$('fields').append(line);}field('processed','Заявка обработана',data.processed,'checkbox');}
if(kind==='articles'){const a=data.article||{};required(field('title','Заголовок',a.title),240);const slug=required(field('slug','Адрес статьи: латиница, цифры и дефисы',a.slug),160);slug.pattern='[a-z0-9]+(?:-[a-z0-9]+)*';field('excerpt','Краткое описание',a.excerpt,'textarea').maxLength=2000;imageField('coverMediaId','Обложка',a.cover?.id);field('status','Публикация',a.status||'DRAFT','select',[['DRAFT','Черновик'],['PUBLISHED','Опубликовать']]);const date=field('publishedAt','Дата публикации (пусто — сейчас или прежняя дата)',a.publishedAt?localDate(a.publishedAt):'','datetime-local');date.step=1;const cat=field('categoryIds','Категории (можно выбрать несколько)','','select',categories.map(c=>[String(c.id),c.name]));cat.multiple=true;for(const c of a.categories||[])if(!categories.some(existing=>existing.id===c.id))cat.append(new Option(c.name,String(c.id)));for(const o of cat.options)o.selected=(a.categories||[]).some(c=>String(c.id)===o.value);field('readingMinutes','Минуты чтения (пусто — автоматически)',id?a.readingMinutes:'','number').min=1;field('seoTitle','Заголовок для поисковиков',data.seoTitle).maxLength=240;field('seoDescription','Описание для поисковиков',data.seoDescription,'textarea').maxLength=500;$('fields').append(el('h3','Содержимое статьи'));const blocks=el('div');blocks.id='blocks';$('fields').append(blocks);for(const b of data.blocks||[])addBlock(b);$('fields').append(button('+ Добавить блок',()=>addBlock({type:'PARAGRAPH'})));}
$('editor').showModal();if(['content','articles'].includes(kind))loadEditorChoices(current);}catch(e){notify(e.message);}}
async function loadEditorChoices(editing){
    const status=el('p','Загружаем список картинок'+(editing.kind==='articles'?' и категорий':'')+'…');
    $('fields').prepend(status);
    const imageSelects=[...$('fields').querySelectorAll('.image-select')];
    const categorySelect=$('fields').querySelector('[name=categoryIds]');
    imageSelects.forEach(n=>n.disabled=true);if(categorySelect)categorySelect.disabled=true;
    const requests=[api(path('media'))];if(categorySelect)requests.push(api(path('categories')));
    const results=await Promise.allSettled(requests);
    if(current!==editing||!$('editor').open||!auth)return;
    const failures=[];
    if(results[0].status==='fulfilled'){
        const selectedIds=[...$('fields').querySelectorAll('.image-select')].map(n=>n.value);
        const uploadedSelections=media.filter(m=>selectedIds.includes(String(m.id))&&!results[0].value.some(v=>v.id===m.id));
        media=[...results[0].value,...uploadedSelections];
        for(const n of $('fields').querySelectorAll('.image-select')){
            const selected=n.value;n.replaceChildren(new Option('Не выбрано',''));
            media.forEach(m=>n.append(new Option(m.originalName+' — '+m.altText,String(m.id))));
            if(selected&&!media.some(m=>String(m.id)===selected))n.append(new Option('Выбранная картинка',selected));
            n.value=selected;n.dispatchEvent(new Event('change'));
        }
    }else failures.push('Картинки: '+results[0].reason.message);
    if(categorySelect){
        if(results[1].status==='fulfilled'){
            const selected=[...categorySelect.selectedOptions].map(o=>o.value);
            const previous=[...categorySelect.options].map(o=>({id:o.value,name:o.text}));
            categories=results[1].value;categorySelect.replaceChildren();
            for(const c of categories)categorySelect.append(new Option(c.name,String(c.id),false,selected.includes(String(c.id))));
            for(const c of previous)if(selected.includes(c.id)&&!categories.some(v=>String(v.id)===c.id))categorySelect.append(new Option(c.name,c.id,false,true));
        }else failures.push('Категории: '+results[1].reason.message);
        categorySelect.disabled=false;
    }
    $('fields').querySelectorAll('.image-select').forEach(n=>n.disabled=false);
    status.replaceChildren();
    if(failures.length){status.append(el('span',failures.join('\n')+' '),button('Повторить загрузку списков',()=>{status.remove();loadEditorChoices(editing);}));}
    else status.remove();
}
function localDate(s){const d=new Date(s);return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,19);}
function addBlock(data){const block=el('div',undefined,'block'),type=el('select');const types={PARAGRAPH:'Текст',HEADING:'Заголовок',QUOTE:'Цитата',BULLET_LIST:'Список',NUMBERED_LIST:'Нумерованный список',IMAGE:'Картинка'};for(const [v,t] of Object.entries(types))type.append(new Option(t,v));type.value=data.type;type.className='blockType';const controls=el('div',undefined,'toolbar');controls.append(button('↑',()=>{if(block.previousElementSibling)block.parentNode.insertBefore(block,block.previousElementSibling);}),button('↓',()=>{if(block.nextElementSibling)block.parentNode.insertBefore(block.nextElementSibling,block);}),button('Убрать',()=>block.remove()));const fields=el('div');block.append(type,fields,controls);function draw(b={}){fields.replaceChildren();function input(key,title,value,tag='input'){const label=el('label',title),n=el(tag);n.dataset.key=key;n.value=value??'';label.append(n);fields.append(label);return n;}if(type.value==='IMAGE'){const n=input('mediaId','Картинка',b.mediaId,'select');n.append(new Option('Выберите картинку',''));media.forEach(m=>n.append(new Option(m.originalName,String(m.id))));if(b.mediaId&&!media.some(m=>m.id===b.mediaId))n.append(new Option('Выбранная картинка',String(b.mediaId)));n.value=b.mediaId||'';n.required=true;attachImageUpload(n);input('alt','Описание',b.alt);input('caption','Подпись',b.caption);}else if(type.value.includes('LIST')){input('items','Каждый пункт с новой строки',(b.items||[]).join('\n'),'textarea').required=true;}else{const t=input('text','Текст',b.text,'textarea');t.required=true;t.maxLength=20000;if(type.value==='HEADING'){const level=input('level','Уровень заголовка',b.level||2,'select');[2,3,4].forEach(v=>level.append(new Option('H'+v,String(v))));level.value=b.level||2;}}}type.onchange=()=>draw();draw(data);$('blocks').append(block);}
function values(){const out={};for(const n of $('fields').querySelectorAll('[name]')){out[n.name]=n.type==='checkbox'?n.checked:n.multiple?[...n.selectedOptions].map(o=>Number(o.value)):n.value;}return out;}
function blocksValue(){return [...$('blocks').children].map(block=>{const b={type:block.querySelector('.blockType').value};for(const n of block.querySelectorAll('[data-key]')){const k=n.dataset.key;b[k]=k==='items'?n.value.split('\n').map(v=>v.trim()).filter(Boolean):['level','mediaId'].includes(k)?Number(n.value):n.value;}return b;});}
function lock(value){busy=value;for(const b of $('editForm').querySelectorAll('button'))b.disabled=value;}
$('editForm').onsubmit=async e=>{e.preventDefault();if(busy)return;const {kind,id,data}=current,v=values();lock(true);$('editError').textContent='';try{let body=v,method=id?'PUT':'POST';if(kind==='content'){v.mediaId=v.mediaId?Number(v.mediaId):null;v.sortOrder=Number(v.sortOrder);}if(kind==='categories'){v.sortOrder=Number(v.sortOrder);if(id)v.version=data.version;}if(kind==='leads'){body={processed:v.processed};method='PATCH';}if(kind==='media'&&!id){body=new FormData();const file=$('fields').querySelector('[name=file]').files[0];if(file.size>15*1024*1024)throw Error('Файл больше 15 МБ. Выберите меньшую картинку.');body.append('file',file);body.append('altText',v.altText);}if(kind==='articles'){v.coverMediaId=v.coverMediaId?Number(v.coverMediaId):null;v.readingMinutes=v.readingMinutes?Number(v.readingMinutes):null;v.publishedAt=v.publishedAt?new Date(v.publishedAt).toISOString():null;v.blocks=blocksValue();if(id)v.version=data.article.version;if(v.status==='PUBLISHED'&&(!v.excerpt.trim()||!v.coverMediaId||!v.categoryIds.length||!v.blocks.length))throw Error('Для публикации добавьте описание, обложку, категорию и содержимое.');}await api(path(kind)+(id?'/'+id:''),method,body);$('editor').close();await load();notify('Сохранено.',true);}catch(e){$('editError').textContent=e.message;}finally{lock(false);}};
$('delete').onclick=async()=>{if(busy||!confirm('Удалить запись навсегда?'))return;lock(true);try{await api(path(current.kind)+'/'+current.id,'DELETE');$('editor').close();await load();notify('Запись удалена.',true);}catch(e){$('editError').textContent=e.message;}finally{lock(false);}};
